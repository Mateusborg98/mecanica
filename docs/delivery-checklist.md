# Checklist de entrega — Fase 3

Este roteiro reúne as validações finais dos quatro repositórios. As evidências
devem ocultar CPF/CNPJ, JWT, senhas, chaves e credenciais AWS.

## 1. Repositórios e governança

- [ ] Exibir os quatro repositórios no GitHub.
- [ ] Exibir `main` e `homolog` protegidas em cada repositório.
- [ ] Comprovar exigência de Pull Request e check obrigatório.
- [ ] Exibir CI verde nas features e CD verde em `homolog`.
- [ ] Promover `homolog` para `main` por Pull Request.

Repositórios esperados:

1. `mecanica-auth-lambda`;
2. `mecanica-k8s-infra`;
3. `mecanica-database-infra`;
4. `mecanica-api`.

## 2. Infraestrutura AWS

```powershell
aws sts get-caller-identity
aws eks describe-cluster --region us-east-1 --name mecanica-homolog --query "cluster.status" --output text
aws eks list-nodegroups --region us-east-1 --cluster-name mecanica-homolog
aws rds describe-db-instances --region us-east-1 --db-instance-identifier mecanica-homolog-postgresql --query "DBInstances[0].{Status:DBInstanceStatus,Public:PubliclyAccessible,Encrypted:StorageEncrypted}" --output table
aws lambda get-function --region us-east-1 --function-name mecanica-auth-homolog --query "Configuration.{Runtime:Runtime,State:State,Memory:MemorySize}" --output table
aws apigatewayv2 get-apis --region us-east-1 --query "Items[?Name=='mecanica-auth-homolog'].[Name,ApiEndpoint]" --output table
```

- [ ] EKS `ACTIVE` e Managed Node Group existente.
- [ ] RDS `available`, privado e criptografado.
- [ ] Lambda Java 21 ativa.
- [ ] API Gateway disponível.
- [ ] States separados armazenados no S3.

## 3. Autenticação ponta a ponta

Use somente o cliente fictício preparado pela migration de homologação.

```powershell
$authBody = @{ cpfCnpj = "<documento-ficticio>" } | ConvertTo-Json
$auth = Invoke-RestMethod -Method Post -Uri "<authentication-endpoint>" -ContentType "application/json" -Body $authBody

Invoke-WebRequest -Uri "<application-url>/ordens-servico" -SkipHttpErrorCheck

$headers = @{ Authorization = "Bearer $($auth.accessToken)"; "X-Correlation-ID" = "evidencia-fase3" }
Invoke-RestMethod -Uri "<application-url>/ordens-servico" -Headers $headers
```

- [ ] Documento inválido é rejeitado.
- [ ] Cliente inexistente ou inativo não recebe token.
- [ ] Token possui assinatura RS256, emissor e expiração válidos.
- [ ] Rota protegida rejeita chamada sem JWT.
- [ ] A mesma rota funciona com JWT válido.
- [ ] CPF/CNPJ não aparece nas claims nem nos logs.

## 4. Aplicação e Kubernetes

```powershell
kubectl get nodes
kubectl get deployment,pods,service,hpa -n mecanica
kubectl rollout status deployment/mecanica-api -n mecanica --timeout=10m
kubectl get --raw /apis/metrics.k8s.io/v1beta1/nodes
```

- [ ] Pod da API em `Running` e `Ready`.
- [ ] Service possui endereço externo.
- [ ] Startup, readiness e liveness probes funcionando.
- [ ] HPA com mínimo, máximo e métricas de CPU/memória.
- [ ] Swagger, OpenAPI, health e uma operação funcional acessíveis.

## 5. Datadog

```powershell
kubectl get pods,daemonset,deployment -n datadog
helm status datadog-agent -n datadog
```

- [ ] Agent e Cluster Agent em execução.
- [ ] Host/cluster `mecanica-homolog` aparece no Datadog.
- [ ] Logs JSON possuem `correlationId` e não possuem dados sensíveis.
- [ ] Latência HTTP aparece no dashboard.
- [ ] CPU e memória do Kubernetes aparecem no dashboard.
- [ ] Volume diário de ordens aparece no dashboard.
- [ ] Duração média de diagnóstico, execução e finalização aparece.
- [ ] Erros e falhas de integração aparecem.
- [ ] Monitores de uptime, latência, falha de OS e CPU foram criados.

Alguns gráficos ficam vazios até que tráfego e transições de status sejam
gerados. Para a evidência, crie e percorra uma ordem fictícia por todos os
status e aguarde alguns minutos para a ingestão.

## Evidência automatizada de 6 de setembro de 2026

Foi executado em homologação um fluxo real usando o API Gateway, a Lambda, o
RDS e a API no EKS. O token não foi impresso nem salvo.

| Verificação | Resultado |
|---|---|
| Autenticação | `Bearer`, expiração de 900 segundos |
| Ordem fictícia | `beb500f3-c5aa-4c1e-9ae3-e6c7823575a3` |
| Correlação | `evidencia-ciclo-completo-20260906` |
| Estados | `RECEBIDA` até `ENTREGUE` |
| Diagnóstico | aproximadamente 2,52 segundos |
| Execução | aproximadamente 2,49 segundos |
| Finalização | aproximadamente 2,50 segundos |
| Logs | JSON correlacionado em todas as transições |
| HPA | métricas de CPU e memória disponíveis |

Esses identificadores pertencem apenas à massa fictícia de homologação e podem
ser usados para localizar as evidências no Datadog.

## 6. Testes e documentação

- [ ] `mecanica-api`: `./mvnw clean verify` verde, 100 testes e JaCoCo aprovado.
- [ ] `mecanica-auth-lambda`: `./mvnw clean verify` verde, 71 testes e JaCoCo aprovado.
- [ ] Terraform formatado e validado nos três repositórios de infraestrutura.
- [ ] README de cada repositório explica configuração, testes e deploy.
- [ ] Diagrama de arquitetura condiz com os recursos implantados.
- [ ] Swagger/OpenAPI acessível na aplicação.

## 7. Encerramento e custos

Somente depois de capturar todas as evidências:

```powershell
aws rds stop-db-instance --region us-east-1 --db-instance-identifier mecanica-homolog-postgresql
terraform -chdir=infra destroy -auto-approve -var="environment=homolog"
```

O `destroy` deve ser executado no repositório `mecanica-k8s-infra`, com o
backend e as variáveis/roles corretas. Confirme depois que o EKS não existe e o
RDS está `stopped`. Lambda e API Gateway podem permanecer, pois o custo sem
requisições é muito baixo no contexto do laboratório.
