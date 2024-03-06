## SpringBoot WebClient Integration-Test

Inspired by https://codersee.com/integration-tests-webclient-wiremock/

But:

- Instead of Wiremock, we use [javalin](https://javalin.io/) as a test facility
- Tests are self-contained. No json-files in `src/test/resources`
- CI-friendly (do not rely on port 8082 to be free)
- Use [kotest](https://kotest.io/docs/assertions/assertions.html) as assertion library

### Test it

- Start the application with an API_KEY provided (`-Dapi.github.key=<YOUR-TOKEN>`)
- Make the application call the github api by issuing

  http://localhost:8080/api/firstcontributions

