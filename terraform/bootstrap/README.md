# Bootstrap (run once, by hand)

Creates the pieces the pipeline itself needs, in the `vahan-dev` account:
the S3 bucket that holds Terraform state, the GitHub OIDC provider, and two roles
(`github-terraform-plan`, read-only; `github-terraform-apply`, for the `dev` environment).

It cannot be created by the pipeline because the pipeline needs it to exist first.
Everything after this is run by the pipeline.

## Run it

```bash
aws sso login --profile vahan-dev
cd terraform/bootstrap
cp terraform.tfvars.example terraform.tfvars   # then edit github_owner
terraform init
terraform plan        # read this carefully before applying
terraform apply
```

This first run keeps its state in a local file (`terraform.tfstate`, git-ignored).
Next, move that state into the bucket it just created:

1. Add a backend block to `versions.tf`:
   ```hcl
   terraform {
     backend "s3" {
       bucket       = "<state_bucket output>"
       key          = "bootstrap/terraform.tfstate"
       region       = "ap-south-1"
       profile      = "vahan-dev"
       use_lockfile = true
     }
   }
   ```
2. `terraform init -migrate-state`, answer `yes`, then delete the local `terraform.tfstate*` files.
