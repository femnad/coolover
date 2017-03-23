all:
	sed "s@#jar_path#@$(shell pwd)/target/uberjar@" contrib/coolover.in > contrib/coolover
	ln -s $(shell pwd)/contrib/coolover $(HOME)/bin/coolover

jar:
	lein uberjar
