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
{{ include "app.selectorLabels" . }}
{{- if .Values.image.tag }}
app.kubernetes.io/version: {{ .Values.image.tag | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
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
