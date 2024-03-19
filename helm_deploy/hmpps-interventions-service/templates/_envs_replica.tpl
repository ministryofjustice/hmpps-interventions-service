    {{/* vim: set filetype=mustache: */}}
{{/*
Environment variables for reporting containers
*/}}
{{- define "deployment-replica.envs" }}
env:
  - name: SERVER_PORT
    value: "{{ .Values.image.ports.app }}"

  {{ range $key, $value := .Values.env }}
  - name: {{ $key }}
    value: "{{ $value }}"
  {{ end }}

  - name: APPLICATIONINSIGHTS_CONNECTION_STRING
    valueFrom:
      secretKeyRef:
        name: application-insights
        key: connection_string

  - name: SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_INTERVENTIONSCLIENT_CLIENTID
    valueFrom:
      secretKeyRef:
        name: hmpps-auth
        key: interventions-service-client-id.txt

  - name: SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_INTERVENTIONSCLIENT_CLIENTSECRET
    valueFrom:
      secretKeyRef:
        name: hmpps-auth
        key: interventions-service-client-secret.txt

  - name: POSTGRES_URI
    valueFrom:
      secretKeyRef:
        name: hmpps-interventions-postgres14-rds-replica-output
        key: rds_instance_endpoint

  - name: POSTGRES_DB
    valueFrom:
      secretKeyRef:
        name: hmpps-interventions-postgres14-rds-replica-output
        key: database_name

  - name: POSTGRES_USERNAME
    valueFrom:
      secretKeyRef:
        name: hmpps-interventions-postgres14-rds-replica-output
        key: database_username

  - name: POSTGRES_PASSWORD
    valueFrom:
      secretKeyRef:
        name: hmpps-interventions-postgres14-rds-replica-output
        key: database_password

  - name: AWS_SNS_TOPIC_ARN
    valueFrom:
      secretKeyRef:
        name: hmpps-domain-events-topic
        key: topic_arn

  - name: NOTIFY_APIKEY
    valueFrom:
      secretKeyRef:
        name: notify
        key: api_key

  - name: SENTRY_DSN
    valueFrom:
      secretKeyRef:
        name: sentry
        key: service_dsn

  - name: AWS_S3_STORAGE_BUCKET_NAME
    valueFrom:
      secretKeyRef:
        name: storage-s3-bucket
        key: bucket_name

  - name: AWS_S3_NDMIS_BUCKET_NAME
    valueFrom:
      secretKeyRef:
        name: reporting-s3-bucket
        key: bucket_name

  {{- with (index .Values.ingress.hosts 0) }}
  - name: INTERVENTIONSAPI_BASEURL
    value: "https://{{ .host}}"
  {{- end -}}
{{- end -}}
