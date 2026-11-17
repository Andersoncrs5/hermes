# Hermes Observabilidade

Configurações e provisionamento da stack de observabilidade centralizada do Hermes Payment:
- **Métricas**: Prometheus, Micrometer, Exporters (`postgres_exporter`, `redis_exporter`, `cadvisor`, `node_exporter`).
- **Logs**: Grafana Loki e Grafana Alloy.
- **Traces**: Grafana Tempo (OTLP gRPC/HTTP) e Jaeger UI.
- **Visualização**: Dashboards e datasources provisionados no Grafana.
