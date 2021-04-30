SHELL := bash
.SHELLFLAGS := -eu -o pipefail -c

MAKEFLAGS += --no-builtin-rules --warn-undefined-variables

.ONESHELL:
.DELETE_ON_ERROR:

BUILD_DIR := build
JS_DIR := resources/public/assets/js
JAR := schafkopf.jar
JS_TARGET := $(JS_DIR)/manifest.edn

.PHONY: all
all:

.PHONY: build
build: jar

.PHONY: build-cljs
build-cljs: $(JS_TARGET)

.PHONY: jar
jar: $(JAR)

.PHONY: repl
repl:
	clj -M:dev:cljs:repl/cider

.PHONY: clean
clean:
	-rm -rf $(JAR) $(BUILD_DIR) $(JS_DIR)

$(JS_TARGET):
	rm -rf $(JS_DIR)
	clojure -M:cljs:shadow-cljs release app

$(JAR): $(JS_TARGET)
	clojure -X:uberjar :jar $@
