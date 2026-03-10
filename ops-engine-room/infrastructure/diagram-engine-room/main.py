from diagrams import Diagram, Cluster, Edge
from diagrams.aws.compute import AppRunner, ECR, EC2
from diagrams.aws.database import RDS
from diagrams.aws.management import Cloudwatch
from diagrams.aws.storage import S3
from diagrams.aws.network import ALB, NATGateway, Endpoint
from diagrams.aws.general import Client, User
from diagrams.onprem.workflow import Airflow
from diagrams.onprem.vcs import Github
from diagrams.onprem.ci import GithubActions
from diagrams.onprem.container import Docker

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
    "nodesep": "1.5",       # Increased from 1.0 to give more breathing room horizontally
    "ranksep": "3.5",       # Drastically increased from 1.5 to provide ample room for horizontal edge labels
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
        "margin": "30",     # Increased margin further to prevent cluster label collision
    }

def create_positioned_edge(color, label="", style="solid", width="1.2", minlen="2", tailport="", headport=""):
    """Creates a stylized edge with precise positioning controls."""
    return Edge(
        label=label,
        color=color,
        fontcolor=color,
        style=style,
        penwidth=width,
        fontname="Helvetica",
        fontsize="12",
        minlen=minlen,      # Stretches the edge, giving labels room to breathe
        decorate="false",   # Prevents a line from label to edge, keeping it clean
        tailport=tailport,   # Forces the connection point from the source node (e.g., 's' for south)
        headport=headport,   # Forces the connection point to the target node (e.g., 'w' for west)
    )

with Diagram("ServiceHub Architecture (Optimized Private Airflow)", show=False, graph_attr=MAIN_ATTR, direction="LR", filename="servicehub_optimized_private_airflow"):

    with Cluster("External Actors", graph_attr=get_cluster_attr(COLORS["cluster_bg_ext"], "External Actors")):
        user_group = [
            User("End Users\n(Thymeleaf UI)"),
            Client("Data Team\n(Airflow UI)")
        ]

    with Cluster("CI/CD Pipeline", graph_attr=get_cluster_attr(COLORS["cluster_bg_ci"], "Source to Deployment")):
        gh_repo = Github("GitHub Repo\n(Source Code)")
        gh_actions = GithubActions("GitHub Actions\n(Build & S3 Sync)")

    with Cluster("AWS Cloud Environment (Production)", graph_attr=get_cluster_attr(COLORS["cluster_bg_aws"], "AWS Production Account", fontsize="16")):

        # FIX: Replaced invisible cluster with a titled grouping for cleaner layout.
        with Cluster("AWS Regional & Global Services", graph_attr=get_cluster_attr(COLORS["cluster_bg_aws"], "AWS Services outside VPC")):
            ecr = ECR("Amazon ECR\n(Java Image)")
            s3_dags = S3("Amazon S3\n(DAGs Source of Truth)")
            cw_logs = Cloudwatch("CloudWatch\n(Logs & Full Traces)")

        app_runner = AppRunner("AWS App Runner\n(Java Backend)")

        with Cluster("VPC (Virtual Private Cloud)", graph_attr=get_cluster_attr(COLORS["bg"], "VPC Boundary")):
            
            with Cluster("Public Subnets", graph_attr=get_cluster_attr(COLORS["cluster_bg_public"], "Public Subnets (Ingress & Egress)")):
                airflow_alb = ALB("Application Load Balancer\n(Airflow Ingress)")
                nat_gw = NATGateway("NAT Gateway\n(External Internet)")

            with Cluster("VPC Endpoints (AWS Backbone)", graph_attr=get_cluster_attr(COLORS["bg"], "AWS Internal Routing")):
                s3_endpoint = Endpoint("S3 Gateway Endpoint")
                cw_endpoint = Endpoint("CloudWatch Interface Endpoint")

            with Cluster("Private Subnets", graph_attr=get_cluster_attr(COLORS["cluster_bg_private"], "Private Subnets (Compute & Data Tier)")):
                airflow_ec2 = EC2("EC2 Instance\n(Docker Host)")
                airflow_sidecar = Docker("S3 Sync Sidecar\n(aws-cli)")
                airflow_app = Airflow("Airflow Services")
                rds_db = RDS("Amazon RDS\n(PostgreSQL 16)")
                
                # Grouped tightly without directional arrows to keep them clustered cleanly
                airflow_ec2 - airflow_sidecar - airflow_app

    # GitHub Triggers
    user_group[1] >> create_positioned_edge(COLORS["edge_ci"], "Git Push") >> gh_repo
    gh_repo >> create_positioned_edge(COLORS["edge_ci"], "Triggers") >> gh_actions
    
    # App Runner Deployment Flow
    gh_actions >> create_positioned_edge(COLORS["edge_ci"], "Pushes Image", width="1.5") >> ecr
    ecr >> create_positioned_edge(COLORS["edge_ci"], "Auto-Deploy", style="dashed") >> app_runner

    # Airflow DAGs S3 Sync Flow
    gh_actions >> create_positioned_edge(COLORS["edge_ci"], "OIDC Authenticated Push\n(aws s3 sync)", style="solid", width="1.5") >> s3_dags
    
    # CORRECTED: S3 pull initiated by sidecar routing through Gateway Endpoint (Free & Fast)
    airflow_sidecar >> create_positioned_edge(COLORS["edge_ci"], "Cron Sync (Pull)", style="dashed", width="1.5") >> s3_endpoint
    s3_endpoint >> create_positioned_edge(COLORS["edge_ci"], "AWS Internal", style="dashed", width="1.5") >> s3_dags
    airflow_sidecar >> create_positioned_edge(COLORS["edge_logs"], "Shared Docker Volume", style="dotted") >> airflow_app

    # User Traffic
    user_group[0] >> create_positioned_edge(COLORS["edge_user"], "HTTPS (443)", width="2.0") >> app_runner
    
    # ALB Routing Logic
    user_group[1] >> create_positioned_edge(COLORS["edge_user"], "HTTPS (443)", width="1.5") >> airflow_alb
    
    # FIX: Label positioning correction for green ALB-to-EC2 (HTTP).
    # Applied specific tailport and headport to force a cleaner path.
    airflow_alb >> create_positioned_edge(COLORS["edge_user"], "HTTP (8080)\nForwarding", width="1.5", tailport="s", headport="w") >> airflow_ec2

    # Database Connections
    # FIX: Red SQL label positioning fix. Applied a very large 'minlen' to 
    # stretch the edge and used a specific tailport on App Runner to 
    # force a simple, direct path to the RDS instance without complex turns.
    app_runner >> create_positioned_edge(COLORS["edge_data"], "SQL (5432)\nvia VPC Connector", width="2.0", minlen="5", tailport="s") >> rds_db
    airflow_app >> create_positioned_edge(COLORS["edge_data"], "ETL Read/Write", width="2.0") >> rds_db

    # CORRECTED: Observability routes through Interface Endpoint to avoid NAT costs
    app_runner >> create_positioned_edge(COLORS["edge_logs"], "Stream Logs", style="dotted") >> cw_logs
    airflow_ec2 >> create_positioned_edge(COLORS["edge_logs"], "awslogs stream", style="dotted") >> cw_endpoint
    cw_endpoint >> create_positioned_edge(COLORS["edge_logs"], "AWS Internal", style="dotted") >> cw_logs
    rds_db >> create_positioned_edge(COLORS["edge_logs"], "DB Logs", style="dotted") >> cw_logs

    # ACTUAL External Traffic (Remains on NAT Gateway)
    airflow_app >> create_positioned_edge(COLORS["edge_data"], "External APIs /\nWeb Scraping", width="1.5") >> nat_gw

print("Diagram generated successfully as servicehub_optimized_private_airflow.png")