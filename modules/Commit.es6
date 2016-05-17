import moment from 'moment';

class Commit {
    register() {
        this.StringHelper = Java.type('mitb.util.StringHelper');
        this.URL = Java.type('java.net.URL');
        this.GitIO = Java.type('mitb.util.GitIO');
        this.Properties = Java.type('mitb.util.Properties');

        const AsyncHttpClient = Java.type('com.ning.http.client.AsyncHttpClient');
        this.asyncHttpClient = new AsyncHttpClient();
        this.githubUrl = `https://api.github.com/repos/${this.Properties.getValue('repo')}/commits`;
    }

    getHelp(event) {
        helper.respond(event, `Syntax: ${event.getArgs()[0]}`);
    }

    getCommands() {
        return Java.to(['commit', 'lastcommit'], "java.lang.String[]");
    }

    onCommand(commandEvent) {
        var json;
        var args = commandEvent.getArgs();
        var url = this.githubUrl + (args.length ? `/${args[0]}` : '');
        this.asyncHttpClient.prepareGet(url)
            .addHeader('Accept', 'application/vnd.github.v3+json')
            .execute(new com.ning.http.client.AsyncCompletionHandler({
                onCompleted: (response) => {
                    if (args.length) {
                        json = JSON.parse(response.getResponseBody());
                    } else {
                        json = JSON.parse(response.getResponseBody())[0];
                    }

                    if (!json) return;

                    var short = this.GitIO.shorten(new this.URL(json.html_url)).toString();
                    var summary = json.commit.message.split('\n')[0];

                    if (summary > 70)
                        summary = summary.substr(0, 70) + '...';

                    var date = moment(json.commit.committer.date).fromNow();
                    helper.respond(commandEvent, `${this.StringHelper.wrapBold(json.sha.substr(0, 7))}: ${summary} - ${json.author.login} (${date}) ${short}`);
                }
            }));
    }
}

export default new Commit();