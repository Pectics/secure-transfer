.PHONY: check format

check:
	./gradlew check

format:
	./gradlew spotlessApply
