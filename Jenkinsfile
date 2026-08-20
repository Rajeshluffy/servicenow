// ============================================================================
// serivcenow — Jenkins Declarative Pipeline (Docker + Minikube + K8s Job)
//
// Flow:
//   1. Checkout serivcenow and autoFrameX (sibling checkouts — serivcenow
//      depends on autoFrameX's autoframex-api/autoframex-cucumber modules,
//      which only exist via local Maven install; see Dockerfile header)
//   2. Build the Docker image (installs the autoFrameX reactor, then
//      serivcenow, inside the image — see Dockerfile)
//   3. Load the image into the Minikube node (no external registry)
//   4. Sync ServiceNow credentials into a K8s Secret from Jenkins Credentials
//   5. Deploy k8s/test-job.yaml as a one-shot Job and wait for completion
//   6. Pull surefire-reports back out and publish JUnit results
//
// First-time setup checklist
// ──────────────────────────
// Jenkins → Manage Jenkins → Credentials → (global) → Add Credentials
//   Add four "Secret text" entries with these exact IDs:
//   [ ] SERVICE_NOW_USERNAME
//   [ ] SERVICE_NOW_PASSWORD
//   [ ] SERVICE_NOW_CLIENT_ID
//   [ ] SERVICE_NOW_CLIENT_SECRET
//
// Jenkins agent requirements
//   [ ] Docker CLI + a running Minikube node named "minikube" reachable via
//       `docker exec minikube ...` (same setup as the GPN/jenkinsLearning
//       pipelines already running in this environment)
//
// Jenkins → New Item → Pipeline
//   [ ] Pipeline Definition : Pipeline script from SCM
//   [ ] SCM                 : Git → URL of the serivcenow repository
//   [ ] Script Path         : Jenkinsfile   (this file)
// ============================================================================

pipeline {

    agent any

    parameters {
        string(
            name:         'AUTOFRAMEX_REPO',
            defaultValue: 'https://github.com/Rajeshluffy/autoFrameX.git',
            description:  'Git URL of the autoFrameX framework repository'
        )
        string(
            name:         'AUTOFRAMEX_BRANCH',
            defaultValue: 'framework-3.1',
            description:  'Branch or tag to checkout for autoFrameX'
        )
    }

    options {
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '10'))
    }

    environment {
        // -i is required: stages below pipe YAML into "${KUBECTL} apply -f -"
        // (stdin), and `docker exec` does not attach stdin to the container
        // process unless -i is passed — without it, kubectl silently receives
        // nothing and fails with "error: no objects passed to apply".
        KUBECTL = 'docker exec -i minikube /var/lib/minikube/binaries/v1.35.1/kubectl --kubeconfig=/etc/kubernetes/admin.conf'
    }

    stages {

        stage('Checkout serivcenow') {
            steps {
                dir('serivcenow') {
                    checkout scm
                }
            }
        }

        stage('Checkout autoFrameX') {
            steps {
                dir('autoFrameX') {
                    git url: params.AUTOFRAMEX_REPO, branch: params.AUTOFRAMEX_BRANCH
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                // Build context = workspace root, which now contains both
                // serivcenow/ and autoFrameX/ as siblings — see Dockerfile
                // header comment for why.
                sh 'docker build --platform linux/amd64 --provenance=false -f serivcenow/Dockerfile -t servicenow-test:${BUILD_ID} .'
            }
        }

        stage('Load Image into Minikube') {
            steps {
                sh '''
                    docker save -o servicenow-test.tar servicenow-test:${BUILD_ID}
                    docker cp servicenow-test.tar minikube:/servicenow-test.tar
                    docker exec minikube docker load -i /servicenow-test.tar
                    rm servicenow-test.tar
                    docker exec minikube rm /servicenow-test.tar
                '''
            }
        }

        stage('Sync Credentials Secret') {
            steps {
                withCredentials([
                    string(credentialsId: 'SERVICE_NOW_USERNAME',      variable: 'SN_USER'),
                    string(credentialsId: 'SERVICE_NOW_PASSWORD',      variable: 'SN_PASS'),
                    string(credentialsId: 'SERVICE_NOW_CLIENT_ID',     variable: 'SN_CLIENT_ID'),
                    string(credentialsId: 'SERVICE_NOW_CLIENT_SECRET', variable: 'SN_CLIENT_SECRET')
                ]) {
                    sh '''
                        cat serivcenow/k8s/namespace.yaml | ${KUBECTL} apply -f -
                        ${KUBECTL} delete secret servicenow-credentials -n servicenow --ignore-not-found=true
                        ${KUBECTL} create secret generic servicenow-credentials -n servicenow \
                            --from-literal=SERVICE_NOW_USERNAME="$SN_USER" \
                            --from-literal=SERVICE_NOW_PASSWORD="$SN_PASS" \
                            --from-literal=SERVICE_NOW_CLIENT_ID="$SN_CLIENT_ID" \
                            --from-literal=SERVICE_NOW_CLIENT_SECRET="$SN_CLIENT_SECRET"
                    '''
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                sh '''
                    ${KUBECTL} delete job servicenow-test-job -n servicenow --ignore-not-found=true
                    sed -i "s/servicenow-test:latest/servicenow-test:${BUILD_ID}/g" serivcenow/k8s/test-job.yaml
                    cat serivcenow/k8s/test-job.yaml | ${KUBECTL} apply -f -
                '''
            }
        }

        stage('Collect Test Results') {
            steps {
                sh '''
                    # Poll instead of a blind 'kubectl wait --for=condition=complete': that
                    # condition never fires for a FAILED job, so it burns the entire timeout
                    # doing nothing even if the pod died in the first few seconds. Checking
                    # succeeded/failed counts directly exits the moment the job is done.
                    for i in $(seq 1 30); do
                        SUCCEEDED=$(${KUBECTL} get job/servicenow-test-job -n servicenow -o jsonpath='{.status.succeeded}' 2>/dev/null)
                        FAILED=$(${KUBECTL} get job/servicenow-test-job -n servicenow -o jsonpath='{.status.failed}' 2>/dev/null)
                        if [ "$SUCCEEDED" = "1" ] || [ "$FAILED" = "1" ]; then
                            echo "Job finished (succeeded=$SUCCEEDED failed=$FAILED) after ${i}0s"
                            break
                        fi
                        sleep 10
                    done

                    mkdir -p serivcenow/target

                    # Stream surefire-reports out of the Minikube node into the Jenkins workspace
                    docker exec minikube tar -c -C /tmp surefire-reports | tar -x -C serivcenow/target
                '''
            }
        }

    }

    post {
        always {
            junit allowEmptyResults: true, testResults: 'serivcenow/target/surefire-reports/*.xml'
            archiveArtifacts artifacts: 'serivcenow/target/surefire-reports/**', allowEmptyArchive: true
            sh '${KUBECTL} delete job servicenow-test-job -n servicenow --ignore-not-found=true'
        }
        success { echo 'Pipeline complete — ServiceNow API suite passed.' }
        failure { echo 'Pipeline failed — check Console Output and archived surefire-reports.' }
    }

}
