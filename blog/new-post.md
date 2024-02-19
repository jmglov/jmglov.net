Title: {{title|safe}}
Date: {{date|safe}}
Tags: {{tags|join:,|safe}}
Description: {{description|default:In which I FIXME|safe}}
Discuss: {{discuss|default:FIXME|safe}}
{% if image %}Image: assets/{{image}}
Image-Alt: {{image-alt|default:FIXME|safe}}
{% endif %}{% if preview %}Preview: true
{% endif %}

{% if image %}![FIXME ALT TEXT GOES HERE][preview]
[preview]: assets/{{image}} "FIXME HOVER TEXT GOES HERE" width=800px
{% endif %}
{{text|default:Write a blog post here|safe}}
