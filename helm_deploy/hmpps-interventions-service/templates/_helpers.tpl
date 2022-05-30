{{/* vim: set filetype=mustache: */}}
{{/*
Expand the name of the chart.
*/}}
{{- define "app.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "app.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Common labels
*/}}
{{- define "app.labels" -}}
helm.sh/chart: {{ include "app.chart" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/name: {{ include "app.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "app.selectorLabels" -}}
app: {{ include "app.name" . }}-api
release: {{ .Release.Name }}
{{- end }}
{{- define "dashboardApp.selectorLabels" -}}
app: {{ include "app.name" . }}-dashboard
release: {{ .Release.Name }}
{{- end }}
{{- define "performanceReportApp.selectorLabels" -}}
app: {{ include "app.name" . }}-performance-report
release: {{ .Release.Name }}
{{- end }}
{{- define "dataDictionary.selectorLabels" -}}
app: {{ include "app.name" . }}-data-dictionary
release: {{ .Release.Name }}
{{- end }}
