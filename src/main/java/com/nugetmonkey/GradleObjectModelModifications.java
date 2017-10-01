package com.nugetmonkey;

public class GradleObjectModelModifications {
    private String[] additionalProjectDependencies;
    private String[] removedProjectDependencies;

    public String[] getAdditionalProjectDependencies() {
        return additionalProjectDependencies;
    }

    public void setAdditionalProjectDependencies(String[] additionalProjectDependencies) {
        this.additionalProjectDependencies = additionalProjectDependencies;
    }

    public String[] getRemovedProjectDependencies() {
        return removedProjectDependencies;
    }

    public void setRemovedProjectDependencies(String[] removedProjectDependencies) {
        this.removedProjectDependencies = removedProjectDependencies;
    }
}
