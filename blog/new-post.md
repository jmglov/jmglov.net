Title: {{title}}
Date: {{date}}
Tags: {{tags|join:\",\"}}
Description: {{description|default:In which I FIXME}}
Discuss: {{discuss|default:FIXME}}
{% if image %}Image: assets/{{image}}
Image-Alt: {{image-alt|default:FIXME}}
{% endif %}{% if preview %}Preview: true
{% endif %}

{% if image %}![FIXME ALT TEXT GOES HERE][preview]
[preview]: assets/{{image}} "FIXME HOVER TEXT GOES HERE" width=800px
{% endif %}
{{text|default:Write a blog post here}}
