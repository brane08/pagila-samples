from film_server import MAX_LIMIT, _clamp, _like


def test_like_escapes_wildcards_and_backslash():
    assert _like("100%_a\\b") == "100\\%\\_a\\\\b"


def test_like_plain_text_unchanged():
    assert _like("Academy") == "Academy"


def test_clamp_bounds():
    assert _clamp(0) == 1
    assert _clamp(-5) == 1
    assert _clamp(10) == 10
    assert _clamp(10_000) == MAX_LIMIT
