variable "account_id" {
  description = "The AWS account this stack may run in (vahan-dev)."
  type        = string
  default     = "349152668028"
}

variable "region" {
  type    = string
  default = "ap-south-1"
}

variable "aws_profile" {
  description = "SSO profile name on your laptop. Leave null in CI."
  type        = string
  default     = "vahan-dev"
}

variable "github_owner" {
  description = "GitHub user or organisation that owns the repository."
  type        = string
}

variable "github_repo" {
  description = "Repository name, e.g. vahan-oneops."
  type        = string
  default     = "vahan-oneops"
}
