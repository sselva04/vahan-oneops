terraform {
  required_version = ">= 1.10"

  # Same state bucket the bootstrap created, with its own key so dev can be
  # destroyed and recreated without touching anything else.
  backend "s3" {
    bucket       = "vahan-tfstate-349152668028-ap-south-1"
    key          = "envs/dev/terraform.tfstate"
    region       = "ap-south-1"
    encrypt      = true
    use_lockfile = true
  }

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = ">= 5.81"
    }
  }
}

# No profile here: locally the AWS_PROFILE environment variable supplies your SSO login,
# in CI the OIDC role supplies temporary credentials.
provider "aws" {
  region              = var.region
  allowed_account_ids = [var.account_id]

  default_tags {
    tags = {
      Project     = "vahan-oneops"
      Environment = "dev"
      ManagedBy   = "terraform"
    }
  }
}
