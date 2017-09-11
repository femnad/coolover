linkfile = $(HOME)/bin/coolover
version = 0.0.1

all: link jar

link:
	sed "s@#jar_path#@$(shell pwd)/target/uberjar@" contrib/coolover.in > contrib/coolover
	ln -fs $(shell pwd)/contrib/coolover $(linkfile)
	chmod +x $(linkfile)

target/uberjar/coolover-$(version)-standalone.jar: src/coolover/core.clj src/coolover/format_issue.clj
	lein uberjar

jar: target/uberjar/coolover-$(version)-standalone.jar

clean:
	rm -r target

.PHONY: jar
