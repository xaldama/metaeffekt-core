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
package org.metaeffekt.core.inventory.processor.reader;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.metaeffekt.core.inventory.processor.model.*;
import org.metaeffekt.core.inventory.processor.writer.InventoryWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XlsInventoryReader extends AbstractInventoryReader {

    private static final Logger LOG = LoggerFactory.getLogger(XlsInventoryReader.class);

    private static final Pattern SPLIT_COLUMN_PATTERN = Pattern.compile("(.*) \\(split-\\d+\\)");

    @Override
    public Inventory readInventory(InputStream in) throws IOException {
        // strange workaround to allow reading large XSL files.
        org.apache.poi.util.IOUtils.setByteArrayMaxOverride(Integer.MAX_VALUE);

        final POIFSFileSystem fileSystem = new POIFSFileSystem(in);
        final HSSFWorkbook workbook = new HSSFWorkbook(fileSystem);

        final Inventory inventory = new Inventory();

        readArtifactMetaData(workbook, inventory);
        readLicenseMetaData(workbook, inventory);
        readLicenseData(workbook, inventory);
        readComponentPatternData(workbook, inventory);
        readVulnerabilityMetaData(workbook, inventory);
        readCertMetaData(workbook, inventory);
        readInventoryInfo(workbook, inventory);
        readAssetMetaData(workbook, inventory);
        readReportData(workbook, inventory);

        applyModificationsForCompatibility(inventory);

        return inventory;
    }

    protected void readArtifactMetaData(HSSFWorkbook workbook, Inventory inventory) {

        HSSFSheet sheet = workbook.getSheet(WORKSHEET_NAME_ARTIFACT_DATA);

        // supporting alternative sheet names for backward compatibility
        if (sheet == null) sheet = workbook.getSheet("Artifact Inventory");

        if (sheet == null) return;

        final List<Artifact> artifacts = new ArrayList<>();
        inventory.setArtifacts(artifacts);

        final BiConsumer<HSSFRow, ParsingContext> rowConsumer = (row, pc) -> {
            final Artifact artifact = readRow(row, new Artifact(), pc);
            if (artifact.isValid()) {
                artifacts.add(artifact);
            }
        };

        parse(inventory, sheet, rowConsumer, InventorySerializationContext.CONTEXT_KEY_ARTIFACT_DATA);
    }

    protected void readComponentPatternData(HSSFWorkbook workBook, Inventory inventory) {

        final HSSFSheet sheet = workBook.getSheet(WORKSHEET_NAME_COMPONENT_PATTERN_DATA);
        if (sheet == null) return;

        final List<ComponentPatternData> componentPatternData = new ArrayList<>();
        inventory.setComponentPatternData(componentPatternData);

        final BiConsumer<HSSFRow, ParsingContext> rowConsumer = (row, pc) -> {
            final ComponentPatternData cpd = readRow(row, new ComponentPatternData(), pc);
            if (cpd.isValid()) {
                componentPatternData.add(cpd);
            }
        };

        parse(inventory, sheet, rowConsumer, InventorySerializationContext.CONTEXT_KEY_COMPONENT_PATTERN_DATA);
    }

    protected void readVulnerabilityMetaData(HSSFWorkbook workbook, Inventory inventory) {
        workbook.sheetIterator().forEachRemaining(sheet -> {
            if (sheet.getSheetName().startsWith(WORKSHEET_NAME_VULNERABILITY_DATA) ||
                    sheet.getSheetName().startsWith(InventoryWriter.VULNERABILITY_ASSESSMENT_WORKSHEET_PREFIX)) {
                readVulnerabilityMetaData(workbook, inventory, sheet.getSheetName());
            }
        });
    }

    protected void readVulnerabilityMetaData(HSSFWorkbook workbook, Inventory inventory, String sheetName) {
        final HSSFSheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            LOG.warn("Inventory sheet {} not found.", sheetName);
            return;
        }

        final String context = sheetNameToAssessmentContext(sheetName);

        final List<VulnerabilityMetaData> vulnerabilityMetaData = new ArrayList<>();
        inventory.setVulnerabilityMetaData(vulnerabilityMetaData, context);

        final BiConsumer<HSSFRow, ParsingContext> rowConsumer = (row, pc) -> {
            final VulnerabilityMetaData vmd = readRow(row, new VulnerabilityMetaData(), pc);
            if (vmd.isValid()) {
                vulnerabilityMetaData.add(vmd);
            }
        };

        parse(inventory, sheet, rowConsumer, InventorySerializationContext.CONTEXT_KEY_VULNERABILITY_DATA);
    }

    protected void readCertMetaData(HSSFWorkbook workbook, Inventory inventory) {

        HSSFSheet sheet = workbook.getSheet(WORKSHEET_NAME_ADVISORY_DATA);

        // for backward compatibility
        if (sheet == null) sheet = workbook.getSheet("Cert");

        if (sheet == null) return;

        final List<CertMetaData> certMetadata = new ArrayList<>();
        inventory.setCertMetaData(certMetadata);

        final BiConsumer<HSSFRow, ParsingContext> rowConsumer = (row, pc) -> {
            final CertMetaData cmd = readRow(row, new CertMetaData(), pc);
            if (cmd.isValid()) {
                certMetadata.add(cmd);
            }
        };

        parse(inventory, sheet, rowConsumer, InventorySerializationContext.CONTEXT_KEY_ADVISORY_DATA);
    }

    protected void readInventoryInfo(HSSFWorkbook workbook, Inventory inventory) {

        final HSSFSheet sheet = workbook.getSheet(WORKSHEET_NAME_INVENTORY_INFO);
        if (sheet == null) return;

        final List<InventoryInfo> inventoryInfo = new ArrayList<>();
        inventory.setInventoryInfo(inventoryInfo);

        final BiConsumer<HSSFRow, ParsingContext> rowConsumer = (row, pc) -> {
            final InventoryInfo info = readRow(row, new InventoryInfo(), pc);
            if (info.isValid()) {
                inventoryInfo.add(info);
            }
        };

        parse(inventory, sheet, rowConsumer, InventorySerializationContext.CONTEXT_KEY_ADVISORY_DATA);
    }

    protected void readReportData(HSSFWorkbook workbook, Inventory inventory) {

        final HSSFSheet sheet = workbook.getSheet(WORKSHEET_NAME_REPORT_DATA);
        if (sheet == null) return;

        final List<ReportData> reportData = new ArrayList<>();
        inventory.setReportData(reportData);

        final BiConsumer<HSSFRow, ParsingContext> rowConsumer = (row, pc) -> {
            final ReportData info = readRow(row, new ReportData(), pc);
            if (info.isValid()) {
                reportData.add(info);
            }
        };

        parse(inventory, sheet, rowConsumer, InventorySerializationContext.CONTEXT_KEY_REPORT_DATA);
    }

    protected void readAssetMetaData(HSSFWorkbook workbook, Inventory inventory) {

        final HSSFSheet sheet = workbook.getSheet(WORKSHEET_NAME_ASSET_DATA);
        if (sheet == null) return;

        final List<AssetMetaData> assetMetaDataList = new ArrayList<>();
        inventory.setAssetMetaData(assetMetaDataList);

        final BiConsumer<HSSFRow, ParsingContext> rowConsumer = (row, pc) -> {
            final AssetMetaData amd = readRow(row, new AssetMetaData(), pc);
            if (amd.isValid()) {
                assetMetaDataList.add(amd);
            }
        };

        parse(inventory, sheet, rowConsumer, InventorySerializationContext.CONTEXT_KEY_ASSET_DATA);
    }

    protected void readLicenseMetaData(HSSFWorkbook workbook, Inventory inventory) {

        HSSFSheet sheet = workbook.getSheet(WORKSHEET_NAME_LICENSE_NOTICES_DATA);

        // supporting alternative sheet names for backward compatibility
        if (sheet == null) sheet = workbook.getSheet("Obligation Notices");
        if (sheet == null) sheet = workbook.getSheet("Component Notices");

        if (sheet == null) return;

        final List<LicenseMetaData> licenseMetaDataList = new ArrayList<>();
        inventory.setLicenseMetaData(licenseMetaDataList);

        final BiConsumer<HSSFRow, ParsingContext> rowConsumer = (row, pc) -> {
            final LicenseMetaData info = readRow(row, new LicenseMetaData(), pc);
            if (info.isValid()) {
                licenseMetaDataList.add(info);
            }
        };

        parse(inventory, sheet, rowConsumer, InventorySerializationContext.CONTEXT_KEY_LICENSE_NOTICE_DATA);
    }

    protected void readLicenseData(HSSFWorkbook workbook, Inventory inventory) {

        final HSSFSheet sheet = workbook.getSheet(WORKSHEET_NAME_LICENSE_DATA);
        if (sheet == null) return;

        final List<LicenseData> licenseDataList = new ArrayList<>();
        inventory.setLicenseData(licenseDataList);

        final BiConsumer<HSSFRow, ParsingContext> rowConsumer = (row, pc) -> {
            final LicenseData licenseData = readRow(row, new LicenseData(), pc);
            if (licenseData.isValid()) {
                licenseDataList.add(licenseData);
            }
        };

        parse(inventory, sheet, rowConsumer, InventorySerializationContext.CONTEXT_KEY_LICENSE_DATA);
    }

    private void parse(Inventory inventory, HSSFSheet sheet, BiConsumer<HSSFRow, ParsingContext> rowConsumer, String contextKey) {
        final Function<HSSFRow, ParsingContext> headerConsumer = row -> parseColumns(row);

        final Iterator<?> rows = sheet.rowIterator();
        if (rows.hasNext()) {
            // read header row
            final ParsingContext pc = headerConsumer.apply((HSSFRow) rows.next());

            // read content
            while (rows.hasNext()) {
                rowConsumer.accept((HSSFRow) rows.next(), pc);
            }

            // read formatting data
            final List<String> headerList = pc.columns;
            final InventorySerializationContext serializationContext = inventory.getSerializationContext();
            serializationContext.put(contextKey + ".columnlist", headerList);
            for (int i = 0; i < headerList.size(); i++) {
                int width = sheet.getColumnWidth(i);
                serializationContext.put(contextKey + ".column[" + i + "].width", width);
            }
        }
    }

    protected ParsingContext parseColumns(HSSFRow row) {
        final ParsingContext parsingContainer = new ParsingContext();
        for (int i = 0; i < row.getPhysicalNumberOfCells(); i++) {
            final HSSFCell cell = row.getCell(i);
            if (cell != null) {
                final String value = cell.getStringCellValue();
                parsingContainer.columnsMap.put(i, value);
                parsingContainer.columns.add(value);
            }
        }
        return parsingContainer;
    }

    protected <T extends AbstractModelBase> T readRow(HSSFRow row, T modelBase, ParsingContext parsingContext) {
        final Map<Integer, String> map = parsingContext.columnsMap;
        for (int i = 0; i < map.size(); i++) {
            final String columnName = map.get(i).trim();
            final HSSFCell cell = row.getCell(i);
            final String value = cell != null ? cell.toString() : null;

            if (value != null) {
                final boolean isSplitColumn = isSplitColumn(columnName);
                if (isSplitColumn) {
                    final String splitColumnName = getSplitColumnName(columnName);
                    final String splitColumnValue = modelBase.get(splitColumnName);

                    if (splitColumnValue != null) {
                        modelBase.set(splitColumnName, splitColumnValue + value);
                    } else {
                        modelBase.set(splitColumnName, value);
                    }
                } else {
                    modelBase.set(columnName, value);
                }
            }
        }

        for (String key : modelBase.getAttributes()) {
            final String value = modelBase.get(key);
            if (value != null) {
                modelBase.set(key, value.trim());
            }
        }

        return modelBase;
    }

    private boolean isSplitColumn(String columnName) {
        if (StringUtils.isEmpty(columnName)) {
            return false;
        }

        return columnName.endsWith(")") && SPLIT_COLUMN_PATTERN.matcher(columnName).matches();
    }

    private String getSplitColumnName(String columnName) {
        if (StringUtils.isEmpty(columnName)) {
            return null;
        }

        final Matcher matcher = SPLIT_COLUMN_PATTERN.matcher(columnName);
        if (matcher.matches()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }

}
