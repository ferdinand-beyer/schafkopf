SHELL := bash
.SHELLFLAGS := -eu -o pipefail -c

MAKEFLAGS += --no-builtin-rules --warn-undefined-variables

.ONESHELL:
.DELETE_ON_ERROR:

find_clj = $(shell find $(1) -type f \( -name '*.clj' -or -name '*.cljc' \))
find_cljs = $(shell find $(1) -type f -name '*.clj[sc]')

CLJ_SRC := $(call find_clj,src)
CLJS_SRC := $(call find_cljs,src)

TEST_CLJ_SRC := $(call find_clj,test)
TEST_CLJS_SRC := $(call find_cljs,test)

RES_SRC := $(shell find resources -type f)

BUILD_DIR := build
JS_TARGET_DIR := $(BUILD_DIR)/resources/public/assets/js

JAR := schafkopf.jar
JS_TARGET := $(JS_TARGET_DIR)/manifest.edn

CLJS_TEST_TARGET := $(BUILD_DIR)/test/node/run-tests.js

.PHONY: all
all: build

.PHONY: build
build: jar

.PHONY: build-cljs
build-cljs: $(JS_TARGET)

.PHONY: npm-deps
npm-deps:
	clojure -M:cljs:shadow-cljs npm-deps

.PHONY: jar
jar: $(JAR)

.PHONY: repl
repl:
	clj -M:dev:test:cljs:repl/cider

.PHONY: run
run: $(JAR)
	HOST_PASSWORD=sau \
	COOKIE_STORE_KEY=c2NoYWZrb3Bmc3BpZWxlbg== \
	java -jar $<

.PHONY: test
test: test-clj test-cljs

.PHONY: test-clj
test-clj:
	clojure -M:test:test-runner

.PHONY: test-cljs
test-cljs: $(CLJS_TEST_TARGET)
	node $<

.PHONY: clean
clean:
	-rm -rf $(JAR) $(BUILD_DIR)

$(JAR): deps.edn $(JS_TARGET) $(CLJ_SRC) $(RES_SRC)
	clojure -X:uberjar :jar $@

# TODO: Strip manifest.edn from :entries after build?
$(JS_TARGET): shadow-cljs.edn deps.edn $(CLJS_SRC) | node_modules
	-rm -rf $(JS_TARGET_DIR)
	clojure -M:cljs:shadow-cljs release app

$(CLJS_TEST_TARGET): deps.edn shadow-cljs.edn $(CLJS_SRC) $(TEST_CLJS_SRC) | node_modules
	clojure -M:cljs:shadow-cljs compile node-test

node_modules:
	npm install
