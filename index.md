---
layout: homepage
title: "Snapcoin: Bitcoin for feature phones. Take pictures and send to spend."
register_destination: "https://fathomless-journey-6479.herokuapp.com/register"
carriers:
  carrier: -Carrier-
  verizon: Verizon
  att: AT&amp;T
  sprint: Sprint
  tmobile: T-Mobile
  boost: Boost
---


## ▽▽ Register! ▽▽
{:.register}

<a id="register"></a>

<form action="{{ page.register_destination }}" method="POST" class="signupform">
<label>Phone Number:<br/><input type="text" placeholder="555 123 4567" name="phone_number" /></label><br>
<select name="carrier">
{% for carrier in page.carriers %}
  <option value="{{ carrier[0] }}">{{ carrier[1] }}</option>
{% endfor %}
</select>
<button type="submit">Submit</button>
</form>
