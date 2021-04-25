SHELL := bash
.SHELLFLAGS := -eu -o pipefail -c

MAKEFLAGS += --no-builtin-rules --warn-undefined-variables

.ONESHELL:
.DELETE_ON_ERROR:

BUILD_DIR := build
JS_DIR := resources/public/js

.PHONY: all
all:

.PHONY: build
build: build-cljs

.PHONY: build-cljs
build-cljs:
	clojure -M:build/cljs:shadow-cljs release app

.PHONY: repl
repl:
	clj -M:dev:local:build/cljs:repl/cider

.PHONY: clean
clean:
	rm -rf $(BUILD_DIR) $(JS_DIR)