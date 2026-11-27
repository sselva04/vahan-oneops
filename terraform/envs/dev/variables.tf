variable "account_id" {
  type    = string
  default = "349152668028"
}

variable "region" {
  type    = string
  default = "ap-south-1"
}

variable "instance_type" {
  description = "2 GB of memory is enough for one environment (k3s + app + database)."
  type        = string
  default     = "t3.small"
}

variable "disk_size_gb" {
  type    = number
  default = 30
}

variable "github_sub_prefix" {
  description = "Start of the GitHub OIDC subject for this repository (immutable-id format, same value as in bootstrap)."
  type        = string
  default     = "repo:sselva04@95541329/vahan-oneops@1473112517"
}
