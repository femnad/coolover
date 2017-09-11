# coolover #

Arguably a better way to use JIRA.

## How on Earth? ##

Place an [edn](https://github.com/edn-format/edn) file in `resource/config.edn` to configure access to the desired JIRA instance, e.g.:

    {:credentials {:user "<username>"
                   :password "<password>"
                   :password-eval "<password-eval-command>"}
     :service {:url "<base-url>"}}

where you may put your password or put a command for printing the desired password to the standard output, such as: `pass foo/bar`.

## A Demonstration Perhaps? ##

### Listing Projects ###

    $ coolover list-projects
    Atlassian Cloud - CLOUD
    Atlassian Community - COMMUNITY
    Atlassian Translations - TRANS
    <...>

### Listing Issues For a Project ###

    STRIDE-8: What is the compliance policy with stride? <2017-09-08 16:52:58>
    [https://jira.atlassian.com/browse/STRIDE-8]
    With HipChat Cloud, we keep our message history for rooms, however, we are unable to track individuals chat on 1 to 1 chats. Is there any change in this for data compliance with Stride or will it still be the same?



    STRIDE-7: iOS Share Extension <2017-09-07 13:17:48 - 2017-09-07 14:29:13>
    [https://jira.atlassian.com/browse/STRIDE-7]
    Share web pages and more from your iPhone through the iOS share extension.

    STRIDE-6: Karma <2017-09-07 13:17:23 - 2017-09-07 14:29:25>
    [https://jira.atlassian.com/browse/STRIDE-6]
    Share the love! Give teammates karma.

### Displaying an Issue ###

    $ coolover show-issue -i STRIDE-8
    STRIDE-8: What is the compliance policy with stride? <2017-09-08 16:52:58>
    [https://jira.atlassian.com/browse/STRIDE-8]
    With HipChat Cloud, we keep our message history for rooms, however, we are unable to track individuals chat on 1 to 1 chats. Is there any change in this for data compliance with Stride or will it still be the same?
