all:
	sed "s@#jar_path#@$(shell pwd)/target/uberjar@" contrib/coolover.in > contrib/coolover
	chmod +x contrib/coolover
	ln -fs $(shell pwd)/contrib/coolover $(HOME)/bin/coolover

jar:
	lein uberjar
