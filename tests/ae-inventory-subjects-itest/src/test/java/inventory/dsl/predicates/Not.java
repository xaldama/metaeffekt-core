package inventory.dsl.predicates;

import org.metaeffekt.core.inventory.processor.model.Artifact;

import java.util.function.Predicate;

public class Not implements NamedArtifactPredicate {

    private final NamedArtifactPredicate input;

    private Not(NamedArtifactPredicate input) {
        this.input = input;
    }

    public static NamedArtifactPredicate not(NamedArtifactPredicate input){
        return new Not(input);
    }

    @Override
    public Predicate<Artifact> getArtifactPredicate() {
        return input.getArtifactPredicate().negate();
    }

    @Override
    public String getDescription() {
        return " NOT( "+input.getDescription()+" ) ";
    }
}
