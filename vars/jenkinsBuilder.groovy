#!/usr/bin/env groovy

import skymind.pipelines.projects.ProjectFactory

def call(Map jobConfig) {
    ProjectFactory.getProject(this, jobConfig)
}
