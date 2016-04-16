load('lib/lodash.js');
load('lib/moment.js');

class LastFM {
    constructor() {
    }

    register() {
    }

    getHelp(event) {
        helper.respond(event, `Syntax: lastfm (user) (info|current|recent|topartists|topsongs overall/7day/1month/3month/6month/12month amt)`);
    }

    getCommands() {
        return Java.to(['lastfm', 'music', 'last'], "java.lang.String[]");
    }

    onCommand(commandEvent) {
        if (commandEvent.getArgs().length < 2)
            return;

        var AsyncHttpClient = Java.type('com.ning.http.client.AsyncHttpClient');
        var Properties = Java.type('mitb.util.Properties');
        var StringHelper = Java.type('mitb.util.StringHelper');

        var user = commandEvent.getArgs()[0];
        var asyncHttpClient = new AsyncHttpClient();
        var template = null;
        var url = null;

        switch (commandEvent.getArgs()[1].toLowerCase()) {
            case 'info':
            case 'plays':
            case 'stats':
                template = (ret) => {
                    if (!ret.user) return;
                    var signUp = moment.unix(ret.user.registered['unixtime']).calendar();
                    return `${ret.user.name} has listened to ${StringHelper.wrapBold(ret.user.playcount)} songs since ${StringHelper.wrapBold(signUp)}.`;
                };
                url = `https://ws.audioscrobbler.com/2.0/?method=user.getInfo&user=${user}&api_key=${Properties.getValue('lastfm.api_key')}&format=json`;
                break;

            case 'current':
            case 'song':
            case 'recent':
                var amt;

                if (commandEvent.getArgs().length < 3) {
                    amt = 1;
                } else if(!isNaN(commandEvent.getArgs()[2]) && isFinite(commandEvent.getArgs()[2])) {
                    amt = commandEvent.getArgs()[2];

                    if (amt < 1)
                        return;

                    if (amt > 5)
                        amt = 5;
                }

                template = (ret) => {
                    var track = ret.recenttracks.track[0];
                    if (!track) return;
                    var str = `${amt == 1 ? ((track['@attr'] && track['@attr'].nowplaying) ? 'Currently listening to' : 'Last listened to') : `Last ${amt} songs:`} `;

                    _(ret.recenttracks.track).each((track, i) => {
                        if (i > amt - 1) return;

                        str += StringHelper.wrapBold(`${track.artist['#text']} - ${track.name}`);

                        if (ret.recenttracks.track.length > 1 && i < ret.recenttracks.track.length - 1 && i < amt - 1) {
                            str += ' | ';
                        }
                    });

                    return str;
                };
                url = `https://ws.audioscrobbler.com/2.0/?method=user.getrecenttracks&user=${user}&api_key=${Properties.getValue('lastfm.api_key')}&format=json&limit=${amt}`;
                break;

            case 'topartists':
            case 'toptracks':
            case 'topsongs':
                var time;

                if (commandEvent.getArgs().length < 3) {
                    time = 'overall';
                } else {
                    time = commandEvent.getArgs()[2].toLowerCase();

                    if (time != '7day' && time != '1month' && time != '3month' && time != '6month' && time != '12month')
                        time = 'overall';
                }

                template = (ret) => {
                    var str = '';
                    var loop = [];
                    var fmt = (item) => `${StringHelper.wrapBold(item.name)} (${item.playcount})`;

                    if (commandEvent.getArgs()[1].toLowerCase() == 'topartists') {
                        if (!ret.topartists.artist[0]) return;

                        str = 'Top artists: ';
                        loop = ret.topartists.artist;
                    } else {
                        if (!ret.toptracks.track[0]) return;

                        str = 'Top songs: ';
                        loop = ret.toptracks.track;
                        fmt = (item) => `${StringHelper.wrapBold(`${item.name} - ${item.artist.name}`)} (${item.playcount})`;
                    }

                    _(loop).each((item, i) => {
                        str += fmt(item);

                        if (loop.length > 1 && i < loop.length - 1) {
                            str += ' | ';
                        }
                    });

                    return str;
                };

                url = `https://ws.audioscrobbler.com/2.0/?method=user.get${commandEvent.getArgs()[1].toLowerCase() == 'topartists' ? 'topartists' : 'toptracks'}&user=${user}&api_key=${Properties.getValue('lastfm.api_key')}&format=json&limit=5&period=${time}`;
                break;

            default:
                return;
        }

        asyncHttpClient.prepareGet(url)
            .execute(new com.ning.http.client.AsyncCompletionHandler({
                onCompleted: (response) => {
                    var json = JSON.parse(response.getResponseBody());
                    var resp = template(json);

                    if (resp) {
                        helper.respond(commandEvent, resp);
                    }
                },
                onThrowable: (t) => {}
            }));
    }
}

export default new LastFM();