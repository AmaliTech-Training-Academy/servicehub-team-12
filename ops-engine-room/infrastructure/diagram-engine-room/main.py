from diagrams import Diagram, Cluster, Edge
from diagrams.aws.compute import AppRunner, ECR, EC2
from diagrams.aws.database import RDS
from diagrams.aws.management import Cloudwatch
from diagrams.aws.general import Client, User
from diagrams.onprem.workflow import Airflow
from diagrams.onprem.vcs import Github
from diagrams.onprem.ci import GithubActions

COLORS = {
    "bg": "#FFFFFF",
    "cluster_bg_ext": "#F8F9FA",
    "cluster_bg_ci": "#F0F7FF",
    "cluster_bg_aws": "#F5F5F5",
    "cluster_bg_public": "#E8F5E9",
    "cluster_bg_private": "#FFFDE7",
    "cluster_border": "#B0BEC5",
    "edge_ci": "#1E88E5",
    "edge_user": "#43A047",
    "edge_data": "#E53935",
    "edge_logs": "#90A4AE",
    "text_primary": "#263238",
}

MAIN_ATTR = {
    "splines": "ortho",
    "nodesep": "1.0",
    "ranksep": "1.5",
    "fontname": "Helvetica",
    "fontsize": "20",
    "fontcolor": COLORS["text_primary"],
    "bgcolor": COLORS["bg"],
    "compound": "true",
    "pad": "1.0",
}

def get_cluster_attr(bgcolor, label, fontsize="14"):
    return {
        "style": "rounded,filled",
        "fillcolor": bgcolor,
        "pencolor": COLORS["cluster_border"],
        "penwidth": "1.2",
        "fontname": "Helvetica-Bold",
        "fontsize": fontsize,
        "fontcolor": COLORS["text_primary"],
        "labeljust": "l",
        "margin": "15",
    }

def create_std_edge(color, label="", style="solid", width="1.2"):
    return Edge(
        label=label,
        color=color,
        fontcolor=color,
        style=style,
        penwidth=width,
        fontname="Helvetica",
        fontsize="11"
    )

with Diagram("ServiceHub Sprint Architecture (SSH Push)", show=False, graph_attr=MAIN_ATTR, direction="LR", filename="servicehub_ssh_push_architecture"):

    with Cluster("External Actors", graph_attr=get_cluster_attr(COLORS["cluster_bg_ext"], "External Actors")):
        user_group = [
            User("End Users\n(Thymeleaf UI)"),
            Client("Data Team\n(Airflow UI)")
        ]

    with Cluster("CI/CD Pipeline", graph_attr=get_cluster_attr(COLORS["cluster_bg_ci"], "Source to Deployment")):
        gh_repo = Github("GitHub Repo\n(Source Code)")
        gh_actions = GithubActions("GitHub Actions\n(Build & SSH Push)")

    with Cluster("AWS Cloud Environment (Production)", graph_attr=get_cluster_attr(COLORS["cluster_bg_aws"], "AWS Production Account", fontsize="16")):

        with Cluster("", graph_attr={"style": "invis"}):
            ecr = ECR("Amazon ECR\n(Java Image)")
            cw_logs = Cloudwatch("CloudWatch\n(Logs & Full Traces)")

        app_runner = AppRunner("AWS App Runner\n(Java Backend)")

        with Cluster("VPC (Virtual Private Cloud)", graph_attr=get_cluster_attr(COLORS["bg"], "VPC Boundary")):
            
            with Cluster("Public Subnets", graph_attr=get_cluster_attr(COLORS["cluster_bg_public"], "Public Subnets (Web Facing)")):
                airflow_ec2 = EC2("EC2 Instance\n(Docker Compose)")
                airflow_app = Airflow("Airflow Services")
                airflow_ec2 - airflow_app

            with Cluster("Private Subnets", graph_attr=get_cluster_attr(COLORS["cluster_bg_private"], "Private Subnets (Data Tier)")):
                rds_db = RDS("Amazon RDS\n(PostgreSQL 16)")

    user_group[1] >> create_std_edge(COLORS["edge_ci"], "Git Push") >> gh_repo
    gh_repo >> create_std_edge(COLORS["edge_ci"], "Triggers") >> gh_actions
    
    gh_actions >> create_std_edge(COLORS["edge_ci"], "Pushes Image", width="1.5") >> ecr
    ecr >> create_std_edge(COLORS["edge_ci"], "Auto-Deploy", style="dashed") >> app_runner

    gh_actions >> create_std_edge(COLORS["edge_ci"], "SSH / SCP Push\n(Deploy DAGs)", style="solid", width="1.5") >> airflow_ec2

    user_group[0] >> create_std_edge(COLORS["edge_user"], "HTTPS (443)", width="2.0") >> app_runner
    user_group[1] >> create_std_edge(COLORS["edge_user"], "HTTP (8080)\nvia Whitelisted IP", width="1.5") >> airflow_ec2

    app_runner >> create_std_edge(COLORS["edge_data"], "SQL (5432)\nvia VPC Connector", width="2.0") >> rds_db
    airflow_app >> create_std_edge(COLORS["edge_data"], "ETL Read/Write", width="2.0") >> rds_db

    app_runner >> create_std_edge(COLORS["edge_logs"], "Stream Logs", style="dotted") >> cw_logs
    airflow_ec2 >> create_std_edge(COLORS["edge_logs"], "awslogs stream", style="dotted") >> cw_logs
    rds_db >> create_std_edge(COLORS["edge_logs"], "DB Logs", style="dotted") >> cw_logs

print("Diagram generated successfully as servicehub_ssh_push_architecture.png")