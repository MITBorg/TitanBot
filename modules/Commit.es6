import moment from 'moment';

class Commit {
    register() {
    }

    getHelp(event) {
        helper.respond(event, `Syntax: ${event.getArgs()[0]}`);
    }

    getCommands() {
        return Java.to(['commit', 'lastcommit'], "java.lang.String[]");
    }

    onCommand(commandEvent) {
        var AsyncHttpClient = Java.type('com.ning.http.client.AsyncHttpClient');
        var StringHelper = Java.type('mitb.util.StringHelper');
        var URL = Java.type('java.net.URL');
        var asyncHttpClient = new AsyncHttpClient();

        asyncHttpClient.prepareGet(`https://api.github.com/repos/${Java.type('mitb.util.Properties').getValue('repo')}/commits`)
            .addHeader('Accept', 'application/vnd.github.v3+json')
            .execute(new com.ning.http.client.AsyncCompletionHandler({
                onCompleted: (response) => {
                    var json = JSON.parse(response.getResponseBody())[0];

                    if (!json) return;

                    var short = Java.type('mitb.util.GitIO').shorten(new URL(json.html_url)).toString();
                    var summary = json.commit.message.split('\n')[0];

                    if (summary > 70)
                        summary = summary.substr(0, 70) + '...';

                    var date = moment(json.commit.committer.date).fromNow();
                    helper.respond(commandEvent, `${StringHelper.wrapBold(json.sha.substr(0, 7))}: ${summary} - ${json.commit.author.name} (${date}) ${short}`);
                },
                onThrowable: (t) => {}
            }));
    }
}

export default new Commit();