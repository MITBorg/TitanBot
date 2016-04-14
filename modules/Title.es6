class Title {
    register() {
        helper.register(engine, 'onMessage', Java.type('mitb.event.events.MessageEvent'));
        this.regex = /(?:(?:(?:https?):\/\/)|(?:www\.))(?:\S+(?::\S*)?@)?(?:(?!(?:10|127)(?:\.\d{1,3}){3})(?!(?:169\.254|192\.168)(?:\.\d{1,3}){2})(?!172\.(?:1[6-9]|2\d|3[0-1])(?:\.\d{1,3}){2})(?:[1-9]\d?|1\d\d|2[01]\d|22[0-3])(?:\.(?:1?\d{1,2}|2[0-4]\d|25[0-5])){2}(?:\.(?:[1-9]\d?|1\d\d|2[0-4]\d|25[0-4]))|(?:(?:[a-z\u00a1-\uffff0-9]-*)*[a-z\u00a1-\uffff0-9]+)(?:\.(?:[a-z\u00a1-\uffff0-9]-*)*[a-z\u00a1-\uffff0-9]+)*(?:\.(?:[a-z\u00a1-\uffff]{2,}))\.?)(?::\d{2,5})?(?:[/?#]\S*)?/ig;
    }

    getHelp(event) {
        helper.respond(event, 'Retrieves titles from a pasted URL.');
    }

    onMessage(event) {
        var AsyncHttpClient = Java.type('com.ning.http.client.AsyncHttpClient');
        var asyncHttpClient = new AsyncHttpClient();

        var urls = [];
        var m;

        while (m = this.regex.exec(event.getSource().getMessage())) {
            urls.push(m[0]);
        }

        if (!urls.length)
            return;

        var queries = '';
        urls.forEach((url) => queries += `select * from html where url='${url.replace('\'', '%27').replace('"', '%22')}' and xpath='//head/title';`);

        var query = `select * from yql.query.multi where queries="${queries}"`;

        asyncHttpClient.prepareGet(`https://query.yahooapis.com/v1/public/yql?q=${encodeURIComponent(query)}&format=json&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys`)
            .execute(new com.ning.http.client.AsyncCompletionHandler({
                onCompleted: (response) => {
                    var json = JSON.parse(response.getResponseBody()).query;

                    var msg = 'Linked: ';

                    var results = json.results.results;

                    if (!Array.isArray(results))
                        results = [results];

                    for (var i = 0; i < results.length; i++) {
                        var obj = results[i];

                        if (obj == null) {
                            msg += Java.type('mitb.util.StringHelper').wrapItalic('Unable to get title. Site down?');
                        } else {
                            msg += Java.type('mitb.util.StringHelper').wrapBold((typeof obj.title == 'string') ? obj.title : obj.title.content);
                        }

                        if (results.length > 1 && i < json.results.results.length - 1) {
                            msg += ' | ';
                        }
                    }

                    event.getSource().respondWith(msg);
                },
                onThrowable: (t) => {}
            }));
    }
}

export default new Title();