# Per-worktree dev lifecycle. Host ports are isolated per worktree by the
# devports skill (.claude/skills/devports) so multiple worktrees run at once.
#
#   make prepare   one-time: parameterize compose ports (commit the result)
#   make up        this worktree: allocate free ports + start the app stack
#   make run       run the Spring app with this worktree's ports
#   make down      stop everything + release this worktree's ports
#
# Run `make help` for the full list.

DP      := node .claude/skills/devports/scripts
COMPOSE := docker compose
JAEGER  := docker compose -f docker-compose.observability-jaeger.yaml
ELASTIC := docker compose -f docker-compose.observability-elastic.yaml

.DEFAULT_GOAL := help
.PHONY: help prepare check allocate status up jaeger elastic run run-jaeger down release

help: ## List targets
	@grep -hE '^[a-zA-Z_-]+:.*?## ' $(MAKEFILE_LIST) | \
	  awk 'BEGIN{FS=":.*?## "}{printf "  \033[36m%-12s\033[0m %s\n",$$1,$$2}'

## ---- one-time project setup (edits tracked files; commit it) ----

prepare: ## Parameterize compose ports + drop container_name (idempotent). App/.http edits are printed for manual review.
	$(DP)/prepare.mts --write

check: ## Is this project prepared? (exit 1 = needs `make prepare`)
	$(DP)/suggest.mts --check

## ---- per worktree (never edits tracked files) ----

allocate: ## Allocate free ports for this worktree -> .env (+ http private env)
	$(DP)/allocate.mts

status: ## Show port reservations across all worktrees
	$(DP)/status.mts

up: allocate ## Allocate + start the app stack (Postgres, Axon Server)
	$(COMPOSE) up -d

jaeger: allocate ## Allocate + start the Jaeger tracing backend
	$(JAEGER) up -d

elastic: allocate ## Allocate + start the Elastic APM tracing backend
	$(ELASTIC) up -d

run: allocate ## Run the Spring app with this worktree's ports exported
	set -a && . ./.env && set +a && ./mvnw spring-boot:run

run-jaeger: allocate ## Run the app with the observability-jaeger profile
	set -a && . ./.env && set +a && ./mvnw spring-boot:run -Dspring-boot.run.profiles=observability-jaeger

down: ## Stop all stacks and release this worktree's ports
	-$(COMPOSE) down --remove-orphans
	-$(JAEGER) down --remove-orphans
	-$(ELASTIC) down --remove-orphans
	$(DP)/release.mts
