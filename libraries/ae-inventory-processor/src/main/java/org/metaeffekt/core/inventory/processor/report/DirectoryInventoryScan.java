/*
 * Copyright 2009-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.metaeffekt.core.inventory.processor.report;

import org.apache.tools.ant.DirectoryScanner;
import org.metaeffekt.core.inventory.processor.command.PrepareScanDirectoryCommand;
import org.metaeffekt.core.inventory.processor.filescan.FileRef;
import org.metaeffekt.core.inventory.processor.filescan.FileSystemScanContext;
import org.metaeffekt.core.inventory.processor.filescan.FileSystemScanExecutor;
import org.metaeffekt.core.inventory.processor.filescan.FileSystemScanParam;
import org.metaeffekt.core.inventory.processor.inspector.MavenJarIdInspector;
import org.metaeffekt.core.inventory.processor.inspector.param.JarInspectionParam;
import org.metaeffekt.core.inventory.processor.inspector.param.ProjectPathParam;
import org.metaeffekt.core.inventory.processor.model.*;
import org.metaeffekt.core.util.ArchiveUtils;
import org.metaeffekt.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.metaeffekt.core.util.FileUtils.*;

/*
    Optimization potential
    - Decouple file expansion from component pattern matching; produce intermediate inventory (indicating expanded artifacts)
    - Execute component patterns in separate step (globally); can be executed independently
    - The reference inventory (with wildcard artifacts and component patterns) could be extractor specific.
 */

public class DirectoryInventoryScan {

    private static final Logger LOG = LoggerFactory.getLogger(DirectoryInventoryScan.class);

    public static final String HINT_SCAN = "scan";
    public static final String HINT_IGNORE = "ignore";

    public static final String DOUBLE_ASTERISK = Constants.ASTERISK + Constants.ASTERISK;

    private Inventory referenceInventory;
    private String[] scanIncludes;

    private String[] scanExcludes;

    private String[] unwrapIncludes;

    private String[] unwrapExcludes;

    private File inputDirectory;

    private File scanDirectory;

    private boolean enableImplicitUnpack = true;

    private boolean includeEmbedded = false;

    public DirectoryInventoryScan(File inputDirectory, File scanDirectory,
                      String[] scanIncludes, String[] scanExcludes, Inventory referenceInventory) {
        this (inputDirectory, scanDirectory,
                scanIncludes, scanExcludes, new String[] { "**/*" },
                new String[0], referenceInventory);
    }

    public DirectoryInventoryScan(File inputDirectory, File scanDirectory,
                                  String[] scanIncludes, String[] scanExcludes,
                                  String[] unwrapIncludes, String[] unwrapExcludes,
                                  Inventory referenceInventory) {
        this.inputDirectory = inputDirectory;

        this.scanDirectory = scanDirectory;
        this.scanIncludes = scanIncludes;
        this.scanExcludes = scanExcludes;

        this.unwrapIncludes = unwrapIncludes;
        this.unwrapExcludes = unwrapExcludes;

        this.referenceInventory = referenceInventory;
    }

    public Inventory createScanInventory() {
        prepareScanDirectory();

        return performScan();
    }

    private void prepareScanDirectory() {
        // if the input directory is not set; the scan directory is not managed
        if (inputDirectory != null) {
            final PrepareScanDirectoryCommand prepareScanDirectoryCommand = new PrepareScanDirectoryCommand();
            prepareScanDirectoryCommand.prepareScanDirectory(inputDirectory, scanDirectory, scanIncludes, scanExcludes);
        }
        LOG.info("Using prepared scan directory: [{}]", scanDirectory);
    }

    public Inventory performScan() {
        // initialize inventories
        Inventory scanInventory = new Inventory();

        // process scanning
        final List<String> assetIdChain = Collections.emptyList();
        scanDirectory(scanDirectory, scanDirectory, scanIncludes, scanExcludes, referenceInventory, scanInventory,
                assetIdChain);

        // remove/merge duplicates
        scanInventory.mergeDuplicates();

        // attempt to extract artifactId, version, groupId from contained POMs
        final Properties properties = new Properties();
        properties.put(ProjectPathParam.KEY_PROJECT_PATH, scanDirectory.getAbsolutePath());
        properties.put(JarInspectionParam.KEY_INCLUDE_EMBEDDED, Boolean.toString(includeEmbedded));
        new MavenJarIdInspector().run(scanInventory, properties);

        return scanInventory;
    }

    private static class MatchResult {
        ComponentPatternData componentPatternData;
        File anchorFile;
        File baseDir;

        MatchResult(ComponentPatternData componentPatternData, File anchorFile, File baseDir) {
            this.componentPatternData = componentPatternData;
            this.anchorFile = anchorFile;
            this.baseDir = baseDir;
        }

        Artifact deriveArtifact(File scanBaseDir) {
            final Artifact derivedArtifact = new Artifact();
            derivedArtifact.setId(componentPatternData.get(ComponentPatternData.Attribute.COMPONENT_PART));
            derivedArtifact.setComponent(componentPatternData.get(ComponentPatternData.Attribute.COMPONENT_NAME));
            derivedArtifact.setVersion(componentPatternData.get(ComponentPatternData.Attribute.COMPONENT_VERSION));
            final String relativePathToBasedir = asRelativePath(scanBaseDir, baseDir);
            derivedArtifact.addProject(relativePathToBasedir);

            if (anchorFile != null) {
                derivedArtifact.set(Constants.KEY_PATH_IN_ASSET, asRelativePath(scanBaseDir, anchorFile));
            }

            // also take over the type attribute
            derivedArtifact.set(Constants.KEY_TYPE, componentPatternData.get(Constants.KEY_TYPE));
            return derivedArtifact;
        }
    }

    /**
     * Scans the directory recursively.
     * <p>
     * All paths are relative to the scanBaseDir.
     *
     * @param scanBaseDir The scan base dir. Use always the same root folder. Also for the recursion.
     * @param scanDir The scan dir. Changes with the recursion.
     * @param scanIncludes
     * @param scanExcludes
     * @param referenceInventory
     * @param scanInventory
     */
    private void scanDirectory(File scanBaseDir, File scanDir, final String[] scanIncludes, final String[] scanExcludes,
           Inventory referenceInventory, Inventory scanInventory, List<String> assetIdChain) {
        LOG.info("{}: scanning...", scanDir);

        // scan the directory using includes and excludes; scans the full tree (maybe not yet unwrapped)
        final String[] filesArray = scanDirectory(scanDir, scanIncludes, scanExcludes);

        // collect all files as list; the list is explicitly created as we later modify the content
        final Set<File> files = new HashSet<>();
        Arrays.stream(filesArray).map(f -> new File(scanDir, f)).forEach(files::add);

        final Map<File, String> normalizedFileMap = new HashMap<>();
        for (File file : files) {
            normalizedFileMap.put(file, normalizePathToLinux(asRelativePath(scanBaseDir, file)));
        }

        LOG.info("{}: matching component patterns", scanDir);
        final List<MatchResult> matchedComponentPatterns =
                matchComponentPatterns(files, normalizedFileMap, scanBaseDir, scanDir, referenceInventory, scanInventory);

        LOG.info("{}: filtering matched files", scanDir);
        final List<MatchResult> matchResultsWithoutFileMatches = new ArrayList<>();
        filterFilesMatchedByComponentPatterns(files, normalizedFileMap, scanBaseDir, matchedComponentPatterns, matchResultsWithoutFileMatches);

        // we need to remove those match results, which did not match any file. Such match results may be caused by
        // generic anchor matches and wildcard anchor checksums.
        if (!matchResultsWithoutFileMatches.isEmpty()) {
            matchedComponentPatterns.removeAll(matchResultsWithoutFileMatches);
        }

        // add artifacts representing the component patterns
        deriveAddonArtifactsFromMatchResult(scanBaseDir, matchedComponentPatterns, scanInventory, assetIdChain);

        // add the files not covered by component patterns
        LOG.info("{}: evaluating / recursing", scanDir);
        populateInventoryWithScannedFiles(scanBaseDir, scanIncludes, scanExcludes, referenceInventory,
                scanInventory, files, assetIdChain);

        LOG.info("{}: scanning completed.", scanDir);
    }

    private void deriveAddonArtifactsFromMatchResult(File scanBaseDir, List<MatchResult> componentPatterns, Inventory scanInventory, List<String> assetIdChain) {
        for (MatchResult matchResult : componentPatterns) {
            final Artifact derivedArtifact = matchResult.deriveArtifact(scanBaseDir);
            applyAssetIdChain(assetIdChain, derivedArtifact);
            scanInventory.getArtifacts().add(derivedArtifact);
        }
    }

    private List<MatchResult> matchComponentPatterns(Set<File> files, Map<File, String> normalizedFileMap, File scanBaseDir, File scanDir, Inventory referenceInventory, Inventory scanInventory) {
        // match component patterns using version anchor version anchors; results in matchedComponentPatterns
        final List<MatchResult> matchedComponentPatterns = new ArrayList<>();

        for (final ComponentPatternData cpd : referenceInventory.getComponentPatternData()) {
            LOG.debug("Checking component pattern: {}", cpd.createCompareStringRepresentation());

            final String anchorChecksum = cpd.get(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM);
            final String versionAnchor = cpd.get(ComponentPatternData.Attribute.VERSION_ANCHOR);
            final String normalizedVersionAnchor = normalizePathToLinux(versionAnchor);

            if (versionAnchor == null) {
                throw new IllegalStateException(String.format("The version anchor of component pattern [%s] must be defined.",
                        cpd.get(ComponentPatternData.Attribute.INCLUDE_PATTERN)));
            }
            if (versionAnchor.contains(DOUBLE_ASTERISK)) {
                throw new IllegalStateException(String.format("The version anchor of component pattern [%s] must not contain **. Use * only.",
                        cpd.get(ComponentPatternData.Attribute.INCLUDE_PATTERN)));
            }
            if (anchorChecksum == null) {
                throw new IllegalStateException(String.format("The version anchor checksum of component pattern [%s] must be defined.",
                        cpd.get(ComponentPatternData.Attribute.INCLUDE_PATTERN)));
            }

            // memorize whether the path fragment of version anchor contains wildcards
            final boolean isVersionAnchorPattern = versionAnchor.contains(Constants.ASTERISK);

            // memorize whether version anchor checksum is specific (and not just *)
            final boolean isVersionAnchorChecksumSpecific = !anchorChecksum.equalsIgnoreCase(Constants.ASTERISK);

            if (versionAnchor.equalsIgnoreCase(Constants.ASTERISK) || versionAnchor.equalsIgnoreCase(Constants.DOT)) {

                if (!anchorChecksum.equalsIgnoreCase(Constants.ASTERISK)) {
                    throw new IllegalStateException(String.format(
                            "The version anchor checksum of component pattern [%s] with version anchor [%s] must be '*'.",
                            cpd.get(ComponentPatternData.Attribute.INCLUDE_PATTERN), versionAnchor));
                }

                final ComponentPatternData copyCpd = new ComponentPatternData(cpd);
                copyCpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, Constants.ASTERISK);
                matchedComponentPatterns.add(new MatchResult(copyCpd, scanDir, scanDir));

                // continue with next component pattern (otherwise this would produce a hugh amount of matched patterns)
                continue;
            }

            final String[] split = normalizedVersionAnchor.split("\\*");
            List<String> quickCheck = Arrays.stream(split).sorted(Comparator.comparingInt(String::length)).collect(Collectors.toList());
            String containsCheckString = quickCheck.get(quickCheck.size() - 1);

            // TODO: we could determine a substring here that does not include any wildcard; must be quick
            //  then we use the contains() to have a quick pre check, before computing the accurate match

            // check whether the version anchor path fragment matches one of the file paths
            for (final File file : files) {
                // generate normalized path relative to scanBaseDir (important; not to scanDir, which may vary as we
                // descend into the hierarchy on recursion)

                final String normalizedPath = normalizedFileMap.get(file);

                if (normalizedPath.contains(containsCheckString)) {
                    if (versionAnchorMatches(normalizedVersionAnchor, normalizedPath, isVersionAnchorPattern)) {

                        // on match infer the checksum of the file
                        final String fileChecksumOrAsterisk = isVersionAnchorChecksumSpecific ? computeChecksum(file) : Constants.ASTERISK;

                        if (!anchorChecksum.equalsIgnoreCase(fileChecksumOrAsterisk)) {
                            LOG.debug("Anchor fileChecksumOrAsterisk mismatch: " + file.getPath());
                            LOG.debug("Expected fileChecksumOrAsterisk :{}; actual file fileChecksumOrAsterisk: {}", anchorChecksum, fileChecksumOrAsterisk);
                        } else {
                            final ComponentPatternData copyCpd = new ComponentPatternData(cpd);
                            copyCpd.set(ComponentPatternData.Attribute.VERSION_ANCHOR_CHECKSUM, fileChecksumOrAsterisk);

                            matchedComponentPatterns.add(new MatchResult(copyCpd, file, computeComponentBaseDir(scanBaseDir, file, normalizedVersionAnchor)));
                        }
                    }
                }
            }
        }
        return matchedComponentPatterns;
    }

    private void populateInventoryWithScannedFiles(final File scanBaseDir,
        final String[] scanIncludes, final String[] scanExcludes,
        final Inventory referenceInventory, final Inventory scanInventory, final Set<File> files,
        final List<String> assetIdChain) {

        for (final File file : files) {
            final String id = file.getName();
            final String checksum = computeMD5Checksum(file);
            final String idFullPath = file.getPath();

            final List<String> errors = new ArrayList<>();

            Artifact artifact = referenceInventory.findArtifactByIdAndChecksum(id, checksum);

            if (artifact == null) {
                artifact = referenceInventory.findArtifactByIdAndChecksum(idFullPath, checksum);
            }

            // match on file name
            if (artifact == null) {
                artifact = referenceInventory.findArtifact(id, true);
                if (!matchesChecksumIfAvailable(artifact, checksum)) {
                    artifact = null;
                }
            }

            // match on file path
            if (artifact == null) {
                artifact = referenceInventory.findArtifact(idFullPath, true);
                if (!matchesChecksumIfAvailable(artifact, checksum)) {
                    artifact = null;
                }
            }

            if (artifact == null) {

                boolean unpacked = false;
                if (enableImplicitUnpack) {

                    // temporary workaround
                    boolean wantToUnpack = true;

                    if (file.getName().toLowerCase().endsWith(".js.gz")) wantToUnpack = false;
                    if (file.getName().toLowerCase().endsWith(".js.map.gz")) wantToUnpack = false;
                    if (file.getName().toLowerCase().endsWith(".css.gz")) wantToUnpack = false;
                    if (file.getName().toLowerCase().endsWith(".css.map.gz")) wantToUnpack = false;
                    if (file.getName().toLowerCase().endsWith(".svg.gz")) wantToUnpack = false;
                    if (file.getName().toLowerCase().endsWith(".json.gz")) wantToUnpack = false;
                    if (file.getName().toLowerCase().endsWith(".ttf.gz")) wantToUnpack = false;
                    if (file.getName().toLowerCase().endsWith(".eot.gz")) wantToUnpack = false;

                    if (wantToUnpack) {

                        // unknown or requires expansion
                        final File targetFolder = new File(file.getParentFile(), "[" + file.getName() + "]");
                        if (unpackIfPossible(file, targetFolder, false, errors)) {
                            scanDirectory(scanBaseDir, targetFolder, scanIncludes, scanExcludes, referenceInventory,
                                    scanInventory, extendAssetIdChain(assetIdChain, file, checksum, scanInventory));
                            unpacked = true;
                        } else {
                            // Not considered as something to unpack
                        }
                    }
                }

                if (!unpacked) {
                    // add new unknown artifact
                    final Artifact newArtifact = new Artifact();
                    newArtifact.setId(id);
                    newArtifact.setChecksum(checksum);

                    // FIXME: we compute hashes, but we do not use them for searching the inventory yet.
                    //   This is however step one of a transition. We currently use id/md5 as central qualifier.
                    newArtifact.set("Hash (SHA-1)", FileUtils.computeSHA1Hash(file));
                    newArtifact.set("Hash (SHA-256)", FileUtils.computeSHA256Hash(file));

                    newArtifact.addProject(asRelativePath(scanBaseDir, file));
                    applyAssetIdChain(assetIdChain, newArtifact);
                    scanInventory.getArtifacts().add(newArtifact);

                    for (String error : errors) {
                        newArtifact.append("Errors", error, ", ");
                    }
                }
            } else {
                artifact.addProject(asRelativePath(scanBaseDir, file));

                // we use the plain id to continue. The rest is sorted out by the report.
                final Artifact copy = new Artifact();
                copy.setId(id);
                copy.setChecksum(checksum);
                copy.set("Hash (SHA-1)", FileUtils.computeSHA1Hash(file));
                copy.set("Hash (SHA-256)", FileUtils.computeSHA256Hash(file));
                copy.addProject(asRelativePath(scanBaseDir, file));

                for (String error : errors) {
                    copy.append("Errors", error, ", ");
                }

                // only include the artifact if the classification does not include HINT_IGNORE
                if (!hasClassification(artifact, HINT_IGNORE)) {
                    applyAssetIdChain(assetIdChain, copy);
                    scanInventory.getArtifacts().add(copy);
                } else {
                    // ISSUE: the collected issues are ignored in this case; we may lose information
                    // if not captured elsewhere
                }

                // in case the artifact contains the scan classification we try to unpack and scan in depth
                if (hasClassification(artifact, HINT_SCAN)) {
                    final File targetFolder = new File(file.getParentFile(), "[" + file.getName() + "]");
                    if (unpackIfPossible(file, targetFolder, true, errors)) {
                        scanDirectory(scanBaseDir, targetFolder, scanIncludes, scanExcludes, referenceInventory,
                                scanInventory, extendAssetIdChain(assetIdChain, file, checksum, scanInventory));
                    } else {
                        // revise exception / error handling
                        throw new IllegalStateException("The artifact with id " + artifact.getId() +
                                " was classified to be scanned in-depth, but cannot be unpacked");
                    }
                }
            }
        }
    }

    private boolean hasClassification(Artifact artifact, String classification) {
        if (StringUtils.hasText(artifact.getClassification())) {
            return artifact.getClassification().contains(classification);
        }
        return false;
    }

    private boolean unpackIfPossible(File file, File targetDir, boolean includeModules, List<String> issues) {
        if (!includeModules) {
            if (file == null || file.getName().toLowerCase().endsWith(".jar")) {
                return false;
            }
        }
        return ArchiveUtils.unpackIfPossible(file, targetDir, issues);
    }

    private void applyAssetIdChain(List<String> assetIdChain, Artifact artifact) {
        if (assetIdChain != null && artifact != null) {
            for (String assetId : assetIdChain) {
                artifact.set(assetId, "x");
            }
        }
    }

    private List<String> extendAssetIdChain(final List<String> assetIdChain, final File archiveFile,
        final String fileChecksum, final Inventory inventory) {
        final List<String> extendedAssetIdChain = new ArrayList<>(assetIdChain);
        final String assetId = "AID-" + archiveFile.getName() + "-" + fileChecksum;
        extendedAssetIdChain.add(assetId);

        AssetMetaData assetMetaData = new AssetMetaData();
        assetMetaData.set(AssetMetaData.Attribute.ASSET_ID, assetId);
        assetMetaData.set("Checksum", fileChecksum);
        assetMetaData.set("File Path", archiveFile.getAbsolutePath());
        inventory.getAssetMetaData().add(assetMetaData);

        return Collections.unmodifiableList(extendedAssetIdChain);
    }

    private void filterFilesMatchedByComponentPatterns(Set<File> files, Map<File, String> normalizedFileMap,
           File scanBaseDir, List<MatchResult> matchedComponentDataOnAnchor, List<MatchResult> matchResultsWithoutFileMatches) {

        // remove the matched file covered by the matched components
        for (MatchResult matchResult : matchedComponentDataOnAnchor) {
            final ComponentPatternData cpd = matchResult.componentPatternData;
            final File anchorFile = matchResult.anchorFile;

            final String versionAnchor = normalizePathToLinux(cpd.get(ComponentPatternData.Attribute.VERSION_ANCHOR));

            final File baseDir = computeComponentBaseDir(scanBaseDir, anchorFile, versionAnchor);

            // build patterns to match (using scanBaseDir relative paths)
            final String baseDirPath = normalizePathToLinux(asRelativePath(scanBaseDir, baseDir));
            final String normalizedIncludePattern = extendIncludePattern(cpd, baseDirPath);
            final String normalizedExcludePattern = normalizePathToLinux(cpd.get(ComponentPatternData.Attribute.EXCLUDE_PATTERN));

            final List<File> matchedFiles = new ArrayList<>();
            for (final File file : files) {
                final String normalizedPath = normalizedFileMap.get(file);
                if (matches(normalizedIncludePattern, normalizedPath)) {
                    if (StringUtils.isEmpty(normalizedExcludePattern) || !matches(normalizedExcludePattern, normalizedPath)) {
                        LOG.debug("Filtered component file: {} for component pattern {}", file, cpd.deriveQualifier());
                        matchedFiles.add(file);
                    }
                }
            }

            if (matchedFiles.isEmpty()) {
                matchResultsWithoutFileMatches.add(matchResult);
            } else {
                files.removeAll(matchedFiles);
            }
        }
    }

    private File computeComponentBaseDir(File scanBaseDir, File anchorFile, String versionAnchor) {
        if (Constants.ASTERISK.equalsIgnoreCase(versionAnchor)) return scanBaseDir;
        if (Constants.DOT.equalsIgnoreCase(versionAnchor)) return scanBaseDir;

        final int versionAnchorFolderDepth = StringUtils.countOccurrencesOf(versionAnchor, "/") + 1;

        File baseDir = anchorFile;
        for (int i = 0; i < versionAnchorFolderDepth; i++) {
            baseDir = baseDir.getParentFile();

            // handle special case the the parent dir does not exist (for whatever reason)
            if (baseDir == null) {
                baseDir = scanBaseDir;
                break;
            }
        }
        return baseDir;
    }

    private String extendIncludePattern(ComponentPatternData cpd, String baseDirPath) {
        String p = cpd.get(ComponentPatternData.Attribute.INCLUDE_PATTERN);
        if (p == null) return p;

        if (StringUtils.isEmpty(baseDirPath)) return p;
        if (Constants.DOT.equals(baseDirPath)) return p;
        if (Constants.DOT_SLASH.equals(baseDirPath)) return p;

        String[] patterns = p.split(",");
        return normalizePathToLinux(Arrays.stream(patterns)
                .map(String::trim)
                .map(s -> baseDirPath + File.separatorChar + s)
                .collect(Collectors.joining(",")));
    }

    private boolean versionAnchorMatches(String normalizedVersionAnchor, String normalizedPath, boolean isVersionAnchorPattern) {
        return (!isVersionAnchorPattern && normalizedPath.endsWith(normalizedVersionAnchor)) ||
                (isVersionAnchorPattern && matches("**/" + normalizedVersionAnchor, normalizedPath));
    }

    private boolean matchesChecksumIfAvailable(Artifact artifact, String checksum) {
        if (artifact == null) return false;
        final String artifactChecksum = artifact.getChecksum();
        if (!StringUtils.hasText(artifactChecksum)) return true; // no checksum available
        return artifactChecksum.equals(checksum);
    }

    // FIXME: DirectoryScanner is not performing very well
    protected String[] scanDirectory(final File directoryToScan, final String[] scanIncludes, final String[] scanExcludes) {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(directoryToScan);
        scanner.setIncludes(scanIncludes);
        scanner.setExcludes(scanExcludes);
        scanner.scan();
        return scanner.getIncludedFiles();
    }

    // FIXME: not yet final; work in progress
    public Inventory scanDirectoryNG(final File directoryToScan) throws IOException {
        final FileSystemScanParam scanParam = new FileSystemScanParam().
                collectAllMatching(scanIncludes, scanExcludes).
                unwrapAllMatching(unwrapIncludes, unwrapExcludes).
                implicitUnwrap(true).
                withReference(referenceInventory);

        LOG.info("Scanning directory {}...", directoryToScan.getAbsolutePath());

        final FileSystemScanContext fileSystemScan = new FileSystemScanContext(new FileRef(directoryToScan), scanParam);
        final FileSystemScanExecutor fileSystemScanExecutor = new FileSystemScanExecutor(fileSystemScan);

        fileSystemScanExecutor.execute();

        // NOTE: at this point, the component is fully unwrapped in the file system (expecting already detected component
        //   patterns).

        return fileSystemScan.getInventory();
    }

    public void setEnableImplicitUnpack(boolean enableImplicitUnpack) {
        this.enableImplicitUnpack = enableImplicitUnpack;
    }

    public void setIncludeEmbedded(boolean includeEmbedded) {
        this.includeEmbedded = includeEmbedded;
    }

}
