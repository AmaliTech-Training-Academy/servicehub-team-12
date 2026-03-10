variable "aws_region" {
  description = "AWS Region"
  type        = string
  default     = "eu-west-1"
}

variable "state_bucket_name" {
  description = "Remote state bucket name"
  type        = string
  default     = "servicehub-state-bucket"
}

