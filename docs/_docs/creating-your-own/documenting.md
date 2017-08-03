---
title: "Documenting environments"
permalink: /creating-your-own/documenting
---

## Welcome page
Carnotzet can generate an HTML documentation page for your environment.

To document your module, add an html fragment at this path : `src/main/resources/welcome/welcome.html`

Here's an example fragment :
```
<h1>Voting result</h1>
<table>
    <tr>
        <th>IP</th>
        <td>${voting-result.ip}</td>
    </tr>
    <tr>
        <th>Link</th>
        <td><a href="http://${voting-result.ip}/">http://${voting-result.ip}/</a></td>
    </tr>
</table>
```

When you run `mvn zet:welcome`, all the fragments will be aggregated in a single page and opened in your default browser. 
Here is an example output:


![Example welcome page]({{ site.baseurl }}{% link _docs/creating-your-own/welcome_example.png %})