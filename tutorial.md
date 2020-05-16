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
    <walkthrough-enable-apis apis="dataflow.googleapis.com,secretmanager.googleapis.com"></walkthrough-enable-apis>  
    Note: If clicking the above button does not successfully enable the APIs after some time, copy the command below 
    and run in the Cloud Shell. 
    ```bash
    gcloud services enable dataflow.googleapis.com secretmanager.googleapis.com
    ```

