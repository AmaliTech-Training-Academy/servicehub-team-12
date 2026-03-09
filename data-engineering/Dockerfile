FROM apache/airflow:3.1.7

USER root
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
USER airflow

WORKDIR /opt/airflow
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

ENV PYTHONPATH=/opt/airflow
ENV AIRFLOW__CORE__DAGS_FOLDER=/opt/airflow/airflow/dags
ENV ETL_TEMP_DIR=/tmp/servicehub_etl

CMD ["python", "etl_pipeline.py"]
