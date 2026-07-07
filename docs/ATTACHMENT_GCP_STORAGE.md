# Attachment storage on Google Cloud Storage

MAWA stores attachment file bytes in Google Cloud Storage. The database keeps only attachment metadata and the object path.

## Runtime configuration

Set these on the `mawa-bes` Cloud Run service:

```text
MAWA_ATTACHMENT_STORAGE_PROVIDER=GCP
MAWA_ATTACHMENT_BUCKET=<gcs-bucket-name>
MAWA_ATTACHMENT_PREFIX=attachments
```

The bucket name is not sensitive and does not need to be in `GCP_SECRET_MAPPINGS`.

## IAM

Grant the Cloud Run service account object access to the bucket:

```bash
gcloud storage buckets add-iam-policy-binding gs://<gcs-bucket-name> \
  --member="serviceAccount:<cloud-run-service-account>" \
  --role="roles/storage.objectAdmin"
```

`roles/storage.objectAdmin` is needed because MAWA creates, reads and deletes attachment objects.

## Database behaviour

New uploads:

```text
attachment.file_path        = GCS object path
attachment.storage_bucket   = GCS bucket
attachment.storage_provider = GCP
attachment.file             = NULL
```

Existing legacy records with `attachment.file` are still readable. Run this endpoint once per tenant to migrate old database blobs to GCS and clear `attachment.file`:

```text
POST /v2/attachment/migrate-to-gcp
```

The endpoint returns:

```json
{ "migrated": 12 }
```


## Object path format

New attachments are stored using this tenant-scoped object path format:

```text
{prefix}/{tenant-host-normalised}/{module-or-type}/{record-id}/{uuid}-{safe-document-name}.{extension}
```

Examples:

```text
attachments/dev-app-mawa-co-za/deposits/DEP-000001/5f1d...-proof-of-payment.pdf
attachments/dev-app-mawa-co-za/partners/PAR-000001/8a4c...-id-copy.pdf
attachments/dev-app-mawa-co-za/invoices/INV-000001/0b62...-invoice.pdf
```

The UUID prefix makes every upload unique. Uploads use a Google Cloud Storage `doesNotExist` precondition so an existing object is never overwritten. Re-uploading a replacement creates a new object path and the database points to the new path.
