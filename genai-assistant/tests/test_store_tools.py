"""Tests for the 6 store MCP tools."""
from store_server import (
    get_customer_store_payments,
    get_store_inventory,
    get_store_monthly_revenue,
    get_store_rentals,
    get_store_top_customers,
    list_stores,
)

STORE_ID_1 = 1
STORE_ID_2 = 2
KNOWN_CUSTOMER_EMAIL = "mary.smith@sakilacustomer.org"


class TestListStores:
    async def test_returns_both_stores(self):
        stores = await list_stores()
        assert len(stores) == 2

    async def test_result_keys(self):
        stores = await list_stores()
        row = stores[0]
        assert {"store_id", "manager", "address", "district", "city"} <= row.keys()

    async def test_store_ids_are_1_and_2(self):
        stores = await list_stores()
        ids = {s["store_id"] for s in stores}
        assert ids == {1, 2}

    async def test_managers_are_named(self):
        stores = await list_stores()
        for store in stores:
            assert store["manager"] and len(store["manager"]) > 1


class TestGetStoreInventory:
    async def test_returns_films_for_valid_store(self):
        results = await get_store_inventory(STORE_ID_1)
        assert len(results) > 0

    async def test_result_keys(self):
        results = await get_store_inventory(STORE_ID_1)
        row = results[0]
        assert {"film_id", "title", "rating", "rental_rate", "category",
                "total_copies", "available_copies"} <= row.keys()

    async def test_available_never_exceeds_total(self):
        results = await get_store_inventory(STORE_ID_1, limit=50)
        for row in results:
            assert row["available_copies"] <= row["total_copies"]

    async def test_category_filter(self):
        results = await get_store_inventory(STORE_ID_1, category="Action")
        assert len(results) > 0
        assert all(r["category"] == "Action" for r in results)

    async def test_category_filter_case_insensitive(self):
        lower = await get_store_inventory(STORE_ID_1, category="action")
        upper = await get_store_inventory(STORE_ID_1, category="Action")
        assert {r["film_id"] for r in lower} == {r["film_id"] for r in upper}

    async def test_unknown_category_returns_empty(self):
        results = await get_store_inventory(STORE_ID_1, category="NotARealCategory")
        assert results == []

    async def test_limit_respected(self):
        results = await get_store_inventory(STORE_ID_1, limit=5)
        assert len(results) <= 5

    async def test_both_stores_have_inventory(self):
        s1 = await get_store_inventory(STORE_ID_1)
        s2 = await get_store_inventory(STORE_ID_2)
        assert len(s1) > 0
        assert len(s2) > 0


class TestGetStoreRentals:
    async def test_returns_rentals(self):
        results = await get_store_rentals(STORE_ID_1)
        assert len(results) > 0

    async def test_result_keys(self):
        results = await get_store_rentals(STORE_ID_1)
        row = results[0]
        assert {"rental_id", "title", "customer", "customer_email",
                "rental_date", "return_date", "is_outstanding"} <= row.keys()

    async def test_sorted_most_recent_first(self):
        results = await get_store_rentals(STORE_ID_1, limit=10)
        dates = [r["rental_date"] for r in results]
        assert dates == sorted(dates, reverse=True)

    async def test_limit_respected(self):
        results = await get_store_rentals(STORE_ID_1, limit=5)
        assert len(results) <= 5

    async def test_is_outstanding_is_boolean(self):
        results = await get_store_rentals(STORE_ID_1, limit=20)
        for row in results:
            assert isinstance(row["is_outstanding"], bool)

    async def test_outstanding_have_no_return_date(self):
        results = await get_store_rentals(STORE_ID_1, limit=50)
        for row in results:
            if row["is_outstanding"]:
                assert row["return_date"] is None
            else:
                assert row["return_date"] is not None


class TestGetStoreTopCustomers:
    async def test_returns_customers(self):
        results = await get_store_top_customers(STORE_ID_1)
        assert len(results) > 0

    async def test_result_keys(self):
        results = await get_store_top_customers(STORE_ID_1)
        row = results[0]
        assert {"customer_id", "customer", "email",
                "rental_count", "total_spent"} <= row.keys()

    async def test_sorted_by_rental_count_descending(self):
        results = await get_store_top_customers(STORE_ID_1, limit=5)
        counts = [r["rental_count"] for r in results]
        assert counts == sorted(counts, reverse=True)

    async def test_all_customers_have_rentals(self):
        results = await get_store_top_customers(STORE_ID_1)
        assert all(r["rental_count"] > 0 for r in results)

    async def test_total_spent_is_non_negative(self):
        results = await get_store_top_customers(STORE_ID_1)
        assert all(float(r["total_spent"]) >= 0 for r in results)

    async def test_limit_respected(self):
        results = await get_store_top_customers(STORE_ID_1, limit=3)
        assert len(results) <= 3


class TestGetCustomerStorePayments:
    async def test_known_customer_returns_record(self):
        result = await get_customer_store_payments(KNOWN_CUSTOMER_EMAIL, STORE_ID_1)
        assert "error" not in result
        assert result["email"].lower() == KNOWN_CUSTOMER_EMAIL.lower()
        assert result["store_id"] == STORE_ID_1

    async def test_result_structure(self):
        result = await get_customer_store_payments(KNOWN_CUSTOMER_EMAIL, STORE_ID_1)
        assert {"customer_id", "name", "email", "store_id",
                "payment_count", "total_paid", "payments"} <= result.keys()

    async def test_payment_entries_have_required_keys(self):
        result = await get_customer_store_payments(KNOWN_CUSTOMER_EMAIL, STORE_ID_1)
        for payment in result["payments"]:
            assert {"payment_id", "amount", "payment_date", "film",
                    "rental_date", "return_date"} <= payment.keys()

    async def test_payment_count_matches_list_length(self):
        result = await get_customer_store_payments(KNOWN_CUSTOMER_EMAIL, STORE_ID_1)
        assert result["payment_count"] == len(result["payments"])

    async def test_total_paid_matches_sum(self):
        result = await get_customer_store_payments(KNOWN_CUSTOMER_EMAIL, STORE_ID_1)
        calculated = sum(float(p["amount"]) for p in result["payments"])
        assert abs(result["total_paid"] - calculated) < 0.01

    async def test_unknown_email_returns_error(self):
        result = await get_customer_store_payments("nobody@nothere.invalid", STORE_ID_1)
        assert "error" in result

    async def test_case_insensitive_email(self):
        lower = await get_customer_store_payments(KNOWN_CUSTOMER_EMAIL.lower(), STORE_ID_1)
        upper = await get_customer_store_payments(KNOWN_CUSTOMER_EMAIL.upper(), STORE_ID_1)
        assert lower["customer_id"] == upper["customer_id"]


class TestGetStoreMonthlyRevenue:
    async def test_returns_monthly_data(self):
        results = await get_store_monthly_revenue(STORE_ID_1)
        assert len(results) > 0

    async def test_result_keys(self):
        row = (await get_store_monthly_revenue(STORE_ID_1))[0]
        assert {"month", "transaction_count", "revenue"} <= row.keys()

    async def test_sorted_most_recent_first(self):
        results = await get_store_monthly_revenue(STORE_ID_1)
        months = [r["month"] for r in results]
        assert months == sorted(months, reverse=True)

    async def test_revenue_is_positive(self):
        results = await get_store_monthly_revenue(STORE_ID_1)
        assert all(float(r["revenue"]) > 0 for r in results)

    async def test_transaction_count_is_positive(self):
        results = await get_store_monthly_revenue(STORE_ID_1)
        assert all(r["transaction_count"] > 0 for r in results)

    async def test_both_stores_have_revenue(self):
        s1 = await get_store_monthly_revenue(STORE_ID_1)
        s2 = await get_store_monthly_revenue(STORE_ID_2)
        assert len(s1) > 0
        assert len(s2) > 0
