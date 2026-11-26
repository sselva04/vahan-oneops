terraform {
  required_version = ">= 1.10" # 1.10+ can lock state with a file in S3, so no DynamoDB table is needed

  # State lives in the bucket this stack created. The profile comes from the AWS_PROFILE
  # environment variable, so the same block works on your laptop and in CI.
  backend "s3" {
    bucket       = "vahan-tfstate-349152668028-ap-south-1"
    key          = "bootstrap/terraform.tfstate"
    region       = "ap-south-1"
    encrypt      = true
    use_lockfile = true
  }

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = ">= 5.81" # 5.81+ no longer needs the GitHub certificate thumbprint
    }
  }
}

provider "aws" {
  region  = var.region
  profile = var.aws_profile # your SSO profile; in CI this is empty and OIDC credentials are used

  # Safety net: refuse to run against any account other than the one we expect.
  allowed_account_ids = [var.account_id]

  default_tags {
    tags = {
      Project   = "vahan-oneops"
      ManagedBy = "terraform"
      Stack     = "bootstrap"
    }
  }
}
