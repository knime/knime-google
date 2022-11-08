#!groovy
def BN = (BRANCH_NAME == 'master' || BRANCH_NAME.startsWith('releases/')) ? BRANCH_NAME : 'releases/2022-12'

library "knime-pipeline@$BN"

properties([
    pipelineTriggers([
        upstream('knime-cloud/' + env.BRANCH_NAME.replaceAll('/', '%2F')),
    ]),
    parameters(workflowTests.getConfigurationsAsParameters()),
    buildDiscarder(logRotator(numToKeepStr: '5')),
    disableConcurrentBuilds()
])

try {
    knimetools.defaultTychoBuild('org.knime.update.google')


    workflowTests.runTests(
        dependencies: [
            repositories: ['knime-google', 'knime-js-base', 'knime-filehandling', 'knime-streaming', 'knime-cloud', 'knime-database',
                'knime-kerberos', 'knime-textprocessing', 'knime-rest', 'knime-xml', 'knime-excel'
            ]
        ]
    )

    stage('Sonarqube analysis') {
        env.lastStage = env.STAGE_NAME
        workflowTests.runSonar()
    }
} catch (ex) {
    currentBuild.result = 'FAILURE'
    throw ex
} finally {
    notifications.notifyBuild(currentBuild.result);
}
/* vim: set shiftwidth=4 expandtab smarttab: */
