output "public_ip" {
  description = "Create a DNS A record for oneops-dev.vahanengg.in pointing here."
  value       = aws_eip.k3s.public_ip
}

output "instance_id" {
  value = aws_instance.k3s.id
}

output "shell_command" {
  description = "Open a shell on the server (no SSH needed)."
  value       = "aws ssm start-session --target ${aws_instance.k3s.id} --profile vahan-dev --region ${var.region}"
}
