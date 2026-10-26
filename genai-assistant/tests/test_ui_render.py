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
