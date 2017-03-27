linkfile = $(HOME)/bin/coolover
version = 0.0.1

all: link jar

link:
	sed "s@#jar_path#@$(shell pwd)/target/uberjar@" contrib/coolover.in > contrib/coolover
	chmod +x contrib/coolover
	ln -fs $(shell pwd)/contrib/coolover $(linkfile)

target/uberjar/coolover-$(version)-standalone.jar: src/coolover/core.clj
	lein uberjar

jar: target/uberjar/coolover-$(version)-standalone.jar

.PHONY: jar
