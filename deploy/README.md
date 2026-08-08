# ping/pong 部署到 Kubernetes(OrbStack)

把 ping-service、pong-service 打成可执行 jar,打进 Docker 镜像,再一键部署到 OrbStack 自带的 K8s。Redis / Kafka / Postgres 也一并放在集群里,全程不用 registry。

## 前置条件

- [OrbStack](https://orbstack.dev) 已装好,并且**在设置里启用 Kubernetes**
- `kubectl` 可用,context 指向 `orbstack`
- JAVA_HOME 指向 zulu-8(见项目根 CLAUDE.md 说明),否则 Maven 会误用 JDK 25

## 1. 打 jar

```bash
cd /Users/bin/uni_java_project/hsbc-ping-pong
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn clean package
```

产物:
- `ping-service/target/ping-service-0.0.1-SNAPSHOT.jar`(~42M,含依赖)
- `pong-service/target/pong-service-0.0.1-SNAPSHOT.jar`(~41M)

> 只想验证打包、不想跑测试,加 `-DskipTests`。

## 2. 打镜像

```bash
docker build -t ping-service:0.0.1 ping-service/
docker build -t pong-service:0.0.1 pong-service/
```

OrbStack 的 K8s 和 Docker 共用容器引擎,本地镜像直接可用,**不需要**推 registry。两个要点:
- 标签**不要用 `latest`**(K8s 对 latest 默认强制重新拉取,本地镜像会被跳过)
- 清单里已设 `imagePullPolicy: IfNotPresent`(双保险)

## 3. 确认 kubectl 指向 OrbStack

```bash
kubectl config current-context   # 应为 orbstack
kubectl get nodes                # 能看到一个 ready 节点
```

## 4. 部署(一次全起)

```bash
kubectl apply -k deploy/k8s
```

等价于按顺序 apply 这些文件:namespace → redis → postgres → kafka → ping-service → pong-service。

```bash
kubectl get pods -n pingpong -w   # 等所有 Pod 到 Running/Ready
kubectl get svc -n pingpong
```

架构:
- `redis` → ping 的跨进程限流(Redis Lua 令牌桶)
- `kafka:9092` → 事件总线(topic `ping-pong-events`)
- `postgres` → pong 落库(`ping_pong_event` 表由 init 脚本在首次启动时建)
- `ping-1:8011`、`ping-2:8022` → 两个 ping 实例,各自 `GET /api/ping/trigger`,通过 `PONG_URL=http://pong-service:8033` 调 pong;两者**共享同一个 Redis 限流桶**,是跨进程限流的活演示
- `pong-service:8033` → `GET /api/pong/ping`

## 5. 验证

```bash
# 起三个转发,分别开三个终端
kubectl port-forward -n pingpong svc/ping-1 8011:8011
kubectl port-forward -n pingpong svc/ping-2 8022:8022
kubectl port-forward -n pingpong svc/pong-service 8033:8033

# 两个 ping 实例各触发一次(可能返回 200 或 429,取决于限流)
curl -i localhost:8011/api/ping/trigger
curl -i localhost:8022/api/ping/trigger
# pong 直接探活
curl -i localhost:8033/api/pong/ping

# 看事件有没有落库
kubectl logs -n pingpong deployment/pong-service --tail=50
```

## 双 ping 实例:跨进程限流演示

两个 ping 实例共用同一个 Redis 令牌桶(key `ping-pong-rate-limit`,上限 2/秒),这是"跨进程限流"的核心演示:

```bash
for i in $(seq 1 6); do
  curl -s -o /dev/null -w "ping-1: %{http_code}\n" localhost:8011/api/ping/trigger
  curl -s -o /dev/null -w "ping-2: %{http_code}\n" localhost:8022/api/ping/trigger
done
```

预期:**合起来**每秒约放行 2 次(2 个 200),其余返回 429。要点:

- 限流是**跨进程**的——ping-1 把桶打光,ping-2 也会被限;各自独立限流的话这轮会全部 200
- Kafka 事件里的 `instance_id` 字段能看出是 `ping-1` 还是 `ping-2` 发的
- 配合 `kubectl logs -n pingpong deployment/pong-service` 能看到事件落库到 `ping_pong_event`

## 常见问题

| 症状 | 处理 |
| --- | --- |
| Pod 一直 `ImagePullBackOff` | 镜像没构建或标签没对上;确认 `docker images` 里有 `ping-service:0.0.1` / `pong-service:0.0.1` |
| ping 日志连不上 redis | `kubectl exec -n pingpong deploy/redis -- redis-cli ping` 先自测;再 `kubectl logs` ping 确认 `REDIS_HOST` 生效 |
| pong 启动报数据库连接失败 | Postgres 还没就绪,Deployment 会自动重启重连;`kubectl logs -n pingpong deploy/postgres` 看 init 脚本是否执行 |
| pong 报表不存在 | `sql/init.sql` 只在 **Postgres 数据目录为空**时执行一次;删 PVC 重来:`kubectl delete pvc -n pingpong pg-data` 后重新 apply |
| `kubectl apply -k` 报 kustomize 错 | 确认当前目录在 `deploy/k8s`,命令为 `kubectl apply -k deploy/k8s`(相对项目根) |

## 面试可讲点

- **镜像**:非 latest 标签 + `imagePullPolicy: IfNotPresent`,解释为什么;基镜像用 Temurin 8 JRE 保持 Java 8 运行时
- **限流跨进程**:ping 的 Redis Lua 令牌桶是这个仓库的核心卖点,夸一下它在多个 ping 副本间共享
- **服务发现**:容器间用 Service DNS(`http://pong-service:8033`),不写死 IP
- **生产差距**(能说清楚加分):密码目前直接写在 env 里,生产应改用 `Secret`;还应加资源 limits、`strategy: RollingUpdate`、PodDisruptionBudget、就绪探针做滚动发布
- **可观测**:每个实例日志带 `instance_id`,方便在日志里区分是哪个副本打的点
