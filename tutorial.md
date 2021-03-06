# Dataflow Secret Manager Tutorial

This guide will show you how to run the example of a data pipeline that access a secret from Secret
Manager. Make sure you meet all the prerequisites listed below.

Click the **Start** button to move to the next step.

## Prerequisites
1.  Choose the GCP project where to run the Dataflow job.
    <walkthrough-project-setup></walkthrough-project-setup>

1.  (Optional) If your current open Cloud Shell project is different from the project you select above.  
    <walkthrough-open-cloud-shell-button></walkthrough-open-cloud-shell-button>  
    Switch to the source code directory.
    ```bash
    cd ~/cloudshell_open/dataflow-secret-manager
    ```

1.  Enable the required APIs.
    <walkthrough-enable-apis apis="dataflow.googleapis.com,secretmanager.googleapis.com,servicenetworking.googleapis.com"></walkthrough-enable-apis>  
    Note: If clicking the above button does not successfully enable the APIs after some time, copy the command below 
    and run in the Cloud Shell. 
    ```bash
    gcloud services enable dataflow.googleapis.com secretmanager.googleapis.com servicenetworking.googleapis.com
    ```

1.  Set up environment variables.
    ```bash
    export PROJECT=$(gcloud info --format='value(config.project)')
    export PROJECT_NUMBER=$(gcloud projects describe $PROJECT --format 'value(projectNumber)')
    export REGION=asia-northeast1
    export DF_REGION=asia-northeast1
    export SECRET_REGION=asia-southeast1
    export GCS_BUCKET=gs://$PROJECT
    ```
    ```bash
    export DB_PASSWORD=<your-password>
    ```

1.  Configure private services access for Cloud SQL.
    ```bash
    gcloud compute addresses create google-managed-services-default \
        --global \
        --purpose=VPC_PEERING \
        --prefix-length=16 \
        --network=default
    ```
    ```bash
    gcloud services vpc-peerings connect \
        --service=servicenetworking.googleapis.com \
        --ranges=google-managed-services-default \
        --network=default
    ```

1.  Create Cloud SQL instance. It will take a while to create.
    ```bash
    gcloud beta sql instances create test \
        --no-assign-ip --region=$REGION --network=default \
        --database-version=SQLSERVER_2017_EXPRESS --cpu=1 --memory=3840MB \
        --root-password=$DB_PASSWORD
    ```

1.  Create a GCS bucket in the current project.
    ```bash
    gsutil mb -l $REGION $GCS_BUCKET
    ```

1.  Create a database using "[Wide World Importers](https://docs.microsoft.com/en-us/sql/samples/wide-world-importers-what-is)" sample database.
    ```bash
    wget https://github.com/microsoft/sql-server-samples/releases/download/wide-world-importers-v1.0/WideWorldImporters-Standard.bak 
    gsutil cp WideWorldImporters-Standard.bak $GCS_BUCKET
    ```
    ```bash
    export CLOUD_SQL_SA=$(gcloud sql instances describe test --format='value(serviceAccountEmailAddress)')
    gsutil iam ch serviceAccount:$CLOUD_SQL_SA:roles/storage.objectViewer $GCS_BUCKET
    gcloud sql import bak test $GCS_BUCKET/WideWorldImporters-Standard.bak \
        --database=wide-world-importers
    ```

1.  Create Dataflow worker service account and grant necessary IAM roles.
    ```bash
    gcloud iam service-accounts create dataflow-worker-sa \
        --display-name="Dataflow worker service account"
    
    export DATAFLOW_WORKER_SA=dataflow-worker-sa@$PROJECT.iam.gserviceaccount.com
    gcloud projects add-iam-policy-binding $PROJECT \
          --member="serviceAccount:$DATAFLOW_WORKER_SA" \
          --role='roles/dataflow.worker'
    gcloud projects add-iam-policy-binding $PROJECT \
          --member="serviceAccount:$DATAFLOW_WORKER_SA" \
          --role='roles/storage.objectAdmin'
    ```

1.  Create JDBC URL secret and grant permission to Dataflow Worker SA.
    ```bash
    export CLOUD_SQL_PRIVATE_IP=$(gcloud sql instances describe test --format='value(ipAddresses[].ipAddress)')
    echo "jdbc:sqlserver://$CLOUD_SQL_PRIVATE_IP;databaseName=wide-world-importers;user=sqlserver;password=$DB_PASSWORD" \
        | gcloud secrets create jdbc-url --locations=$SECRET_REGION \
            --replication-policy=user-managed --data-file=-
    gcloud secrets add-iam-policy-binding jdbc-url \
        --member="serviceAccount:$DATAFLOW_WORKER_SA" \
        --role='roles/secretmanager.secretAccessor'
    ```

1.  Enable Private IP Google Access.
    ```bash
    gcloud compute networks subnets update default \
        --region=$DF_REGION \
        --enable-private-ip-google-access
    ```

## How to Run

1.  Build the application and deploy as Dataflow template.
    ```bash
    export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
    ./gradlew build
    ./gradlew run --args="--project=$PROJECT --region=$DF_REGION --stagingLocation=$GCS_BUCKET/df-temp --tempLocation=$GCS_BUCKET/df-temp --templateLocation=$GCS_BUCKET/templates/main-pipeline --runner=DataflowRunner"
    ```

1.  Run the Dataflow template.
    ```bash
    gcloud dataflow jobs run secret-manager-sample \
        --region=$DF_REGION --worker-region=$DF_REGION --disable-public-ips \
        --service-account-email=$DATAFLOW_WORKER_SA \
        --gcs-location=$GCS_BUCKET/templates/main-pipeline \
        --parameters="jdbcUrlSecretName=projects/$PROJECT_NUMBER/secrets/jdbc-url/versions/latest"
    ```
1.  Open the Dataflow page by accessing the link in your Cloud Shell.
    ```bash
    echo "https://console.cloud.google.com/dataflow?project=$PROJECT"
    ```
    
1.  Navigate to the running Dataflow job and observe that the pipeline can read the data from the database successfully.

## Congratulations

<walkthrough-conclusion-trophy></walkthrough-conclusion-trophy>

You have successfully run a Dataflow pipeline that reads a secret from Secret Manager!

Please let me know how it goes and share with me your experience!
