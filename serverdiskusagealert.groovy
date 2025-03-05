pipeline {
    agent any
    environment {
        INSTANCE_1 = '3.82.105.69'  // Replace with the actual IP of instance 1
        INSTANCE_2 = '3.87.13.157'  // Replace with the actual IP of instance 2
        INSTANCE_3 = '54.161.58.132'  // Replace with the actual IP of instance 3
        LOG_DIR = '/home/karthi/server_monitoring' // Replace with your local log directory
        PEM_FILE_PATH = '/home/karthi/test.pem'  // Path to your PEM file
        REMOTE_USER = 'ubuntu'  // Replace with the username of your EC2 instance
    }
    stages {
        stage('Check Disk Usage on Instances') {
            parallel {
                stage('Check Instance 1') {
                    steps {
                        script {
                            checkDiskUsage(INSTANCE_1, "Procept-Prod") // Alias for instance 1
                        }
                    }
                }
                stage('Check Instance 2') {
                    steps {
                        script {
                            checkDiskUsage(INSTANCE_2, "Procept-Dev") // Alias for instance 2
                        }
                    }
                }
                stage('Check Instance 3') {
                    steps {
                        script {
                            checkDiskUsage(INSTANCE_3, "P-Mac") // Alias for instance 3
                        }
                    }
                }
            }
        }
    }
    post {
        always {
            echo "Disk usage check completed!"
        }
    }
}

def checkDiskUsage(instance, alias) {
    // SSH into the remote instance using the PEM file for authentication
    def diskUsage = sh(script: "ssh -i ${PEM_FILE_PATH} -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null ${REMOTE_USER}@${instance} 'df -h /' | grep -v Filesystem", returnStdout: true).trim()
    def topUsage = sh(script: "ssh -i ${PEM_FILE_PATH} -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null ${REMOTE_USER}@${instance} 'top -bn1 | head -n 10'", returnStdout: true).trim()

    // Print the outputs
    echo "Disk usage for ${instance}:"
    echo "${diskUsage}"
    echo "Top usage for ${instance}:"
    echo "${topUsage}"

    // Parse the disk usage to check if it's over 50%
    def diskUsageLines = diskUsage.split("\n")
    def diskUsagePercentage = 0
    diskUsageLines.each { line ->
        // Ignore the first line (header) and focus on the last column (usage percentage)
        def parts = line.split()
        if (parts.size() > 4) {
            try {
                diskUsagePercentage = parts[4].replace('%', '').toInteger()
            } catch (NumberFormatException e) {
                echo "Skipping non-numeric value: ${parts[4]}"
            }
        }
    }

    // Check if the disk usage exceeds 50% and send an email alert using the default Jenkins mail
    if (diskUsagePercentage > 50) {
        mail to: 'karthikeyanm.aitech@gmail.com',
             subject: "Disk Space Alert for ${instance}",
             body: "Warning: The disk space usage on ${instance} is above 50%. Current usage: ${diskUsagePercentage}%. Please check."
    }

    // Get current date in YYYY-MM-DD format
    def currentDate = sh(script: "date +%F", returnStdout: true).trim()

    // Create the directory for the alias if it doesn't exist
    def aliasDir = "${LOG_DIR}/${alias}"
    sh(script: "mkdir -p ${aliasDir}")  // This ensures the directory exists

    // Save the logs locally inside the alias folder
    writeFile file: "${aliasDir}/disk_usage_${alias}_${currentDate}.log", text: diskUsage
    writeFile file: "${aliasDir}/top_usage_${alias}_${currentDate}.log", text: topUsage
}
