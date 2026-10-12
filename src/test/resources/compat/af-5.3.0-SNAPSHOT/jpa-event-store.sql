-- Fixture for JpaEventStoreDataFromAxon530CompatibilityTest. Do not edit, regenerate with generate-fixture.sh.
-- Written by old-5.3.0-SNAPSHOT.jar (axon-messaging 5.3.0-SNAPSHOT), JPA event store, on postgres:16.13.
-- Scenario: see generate-fixture.sh.
--
-- PostgreSQL database dump
--

\restrict 7uZifbNC689GFBEpPg7CxYUk3Pfub3tPjoVKzz4Jbqbj5nMuggnjfCi4cd9FgR5

-- Dumped from database version 16.13 (Debian 16.13-1.pgdg13+1)
-- Dumped by pg_dump version 16.13 (Debian 16.13-1.pgdg13+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: aggregate-event-global-index-sequence; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public."aggregate-event-global-index-sequence"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: aggregate_event_entry; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.aggregate_event_entry (
    global_index bigint NOT NULL,
    aggregate_identifier character varying(255),
    aggregate_sequence_number bigint,
    aggregate_type character varying(255),
    identifier character varying(255) NOT NULL,
    metadata oid,
    payload oid NOT NULL,
    "timestamp" character varying(255) NOT NULL,
    type character varying(255) NOT NULL,
    version character varying(255) NOT NULL
);


--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


--
-- Name: read_model_built_dwelling; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.read_model_built_dwelling (
    dwelling_id character varying(255) NOT NULL,
    creature_id character varying(255),
    game_id character varying(255)
);


--
-- Name: read_model_dwelling; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.read_model_dwelling (
    dwelling_id character varying(255) NOT NULL,
    available_creatures integer,
    cost_per_troop jsonb,
    creature_id character varying(255),
    game_id character varying(255)
);


--
-- Name: token_entry; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.token_entry (
    processor_name character varying(255) NOT NULL,
    segment integer NOT NULL,
    mask integer NOT NULL,
    owner character varying(255),
    "timestamp" character varying(255) NOT NULL,
    token oid,
    token_type character varying(255)
);


--
-- Name: 16427; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16427');


--
-- Name: 16428; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16428');


--
-- Name: 16429; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16429');


--
-- Name: 16430; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16430');


--
-- Name: 16431; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16431');


--
-- Name: 16432; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16432');


--
-- Name: 16433; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16433');


--
-- Name: 16434; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16434');


--
-- Name: 16435; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16435');


--
-- Name: 16436; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16436');


--
-- Name: 16437; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16437');


--
-- Name: 16438; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16438');


--
-- Name: 16439; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16439');


--
-- Name: 16440; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16440');


--
-- Name: 16441; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16441');


--
-- Name: 16442; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16442');


--
-- Name: 16443; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16443');


--
-- Name: 16444; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16444');


--
-- Name: 16445; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16445');


--
-- Name: 16446; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16446');


--
-- Name: 16447; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16447');


--
-- Name: 16448; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16448');


--
-- Name: 16449; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16449');


--
-- Name: 16450; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16450');


--
-- Name: 16451; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16451');


--
-- Name: 16452; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16452');


--
-- Name: 16453; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16453');


--
-- Name: 16454; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16454');


--
-- Name: 16455; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16455');


--
-- Name: 16456; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16456');


--
-- Name: 16457; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16457');


--
-- Name: 16458; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16458');


--
-- Name: 16459; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16459');


--
-- Name: 16460; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16460');


--
-- Name: 16461; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16461');


--
-- Name: 16462; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16462');


--
-- Name: 16463; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16463');


--
-- Name: 16464; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16464');


--
-- Name: 16465; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16465');


--
-- Name: 16466; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16466');


--
-- Name: 16467; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16467');


--
-- Name: 16468; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16468');


--
-- Name: 16469; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16469');


--
-- Name: 16470; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16470');


--
-- Name: 16471; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16471');


--
-- Name: 16472; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16472');


--
-- Name: 16473; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16473');


--
-- Name: 16474; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16474');


--
-- Name: 16475; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16475');


--
-- Name: 16476; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16476');


--
-- Name: 16477; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16477');


--
-- Name: 16478; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16478');


--
-- Name: 16479; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16479');


--
-- Name: 16480; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16480');


--
-- Name: 16481; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16481');


--
-- Name: 16482; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16482');


--
-- Name: 16483; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16483');


--
-- Name: 16484; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16484');


--
-- Name: 16485; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16485');


--
-- Name: 16486; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16486');


--
-- Name: 16487; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16487');


--
-- Name: 16488; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16488');


--
-- Name: 16489; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16489');


--
-- Name: 16490; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16490');


--
-- Name: 16491; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16491');


--
-- Name: 16492; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16492');


--
-- Name: 16493; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16493');


--
-- Name: 16494; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16494');


--
-- Name: 16495; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16495');


--
-- Name: 16496; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16496');


--
-- Name: 16497; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16497');


--
-- Name: 16498; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16498');


--
-- Name: 16499; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16499');


--
-- Name: 16500; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16500');


--
-- Name: 16501; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16501');


--
-- Name: 16502; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16502');


--
-- Name: 16503; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16503');


--
-- Name: 16504; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16504');


--
-- Name: 16505; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16505');


--
-- Name: 16506; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16506');


--
-- Name: 16507; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16507');


--
-- Name: 16508; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16508');


--
-- Name: 16509; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16509');


--
-- Name: 16510; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16510');


--
-- Name: 16511; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16511');


--
-- Name: 16512; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16512');


--
-- Name: 16513; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16513');


--
-- Name: 16514; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16514');


--
-- Name: 16515; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16515');


--
-- Name: 16516; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16516');


--
-- Name: 16517; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16517');


--
-- Name: 16518; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16518');


--
-- Name: 16519; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16519');


--
-- Name: 16520; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16520');


--
-- Name: 16521; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16521');


--
-- Name: 16522; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16522');


--
-- Name: 16523; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16523');


--
-- Name: 16524; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16524');


--
-- Name: 16525; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16525');


--
-- Name: 16526; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16526');


--
-- Name: 16527; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16527');


--
-- Name: 16528; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16528');


--
-- Name: 16529; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16529');


--
-- Name: 16530; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16530');


--
-- Name: 16531; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16531');


--
-- Name: 16532; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('16532');


--
-- Data for Name: aggregate_event_entry; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.aggregate_event_entry (global_index, aggregate_identifier, aggregate_sequence_number, aggregate_type, identifier, metadata, payload, "timestamp", type, version) FROM stdin;
1	Dwelling:dwelling-1	0	Dwelling	cb7346fd-5a0c-4726-ba80-33efb39de08f	16494	16495	2026-10-12T15:37:52.243Z	com.dddheroes.heroesofddd.creaturerecruitment.events.DwellingBuilt	0.0.1
2	Dwelling:dwelling-2	0	Dwelling	9e5b7826-70ae-407c-9caa-6cbd72108568	16496	16497	2026-10-12T15:37:52.292Z	com.dddheroes.heroesofddd.creaturerecruitment.events.DwellingBuilt	0.0.1
3	Dwelling:dwelling-1	1	Dwelling	a31326c2-2943-4dbe-9fa4-f66064bdd4d7	16500	16501	2026-10-12T15:37:52.318Z	com.dddheroes.heroesofddd.creaturerecruitment.events.AvailableCreaturesChanged	0.0.1
4	Dwelling:dwelling-2	1	Dwelling	8cb50483-9066-454f-ae12-2c968b83768b	16503	16504	2026-10-12T15:37:52.342Z	com.dddheroes.heroesofddd.creaturerecruitment.events.AvailableCreaturesChanged	0.0.1
5	Dwelling:dwelling-1	2	Dwelling	a67da74e-a988-4a6b-b346-0363999cf2b8	16506	16507	2026-10-12T15:37:52.369Z	com.dddheroes.heroesofddd.creaturerecruitment.events.CreatureRecruited	0.0.1
6	Army:army-1	0	Army	623af354-a11d-4d82-9f27-c3ba4876cd08	16509	16510	2026-10-12T15:37:52.387Z	com.dddheroes.heroesofddd.armies.events.CreatureAddedToArmy	0.0.1
7	Dwelling:dwelling-2	2	Dwelling	1e881c77-978c-426a-b4f4-5fd63c253372	16512	16513	2026-10-12T15:37:52.399Z	com.dddheroes.heroesofddd.creaturerecruitment.events.CreatureRecruited	0.0.1
8	Army:army-1	1	Army	35d20bdc-477b-4e79-898d-e54a8743f624	16515	16516	2026-10-12T15:37:52.426Z	com.dddheroes.heroesofddd.armies.events.CreatureAddedToArmy	0.0.1
\.


--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
\.


--
-- Data for Name: read_model_built_dwelling; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.read_model_built_dwelling (dwelling_id, creature_id, game_id) FROM stdin;
Dwelling:dwelling-1	angel	compat-game
Dwelling:dwelling-2	pegasus	compat-game
\.


--
-- Data for Name: read_model_dwelling; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.read_model_dwelling (dwelling_id, available_creatures, cost_per_troop, creature_id, game_id) FROM stdin;
Dwelling:dwelling-1	2	{"GEMS": 1, "GOLD": 3000}	angel	compat-game
Dwelling:dwelling-2	3	{"GOLD": 1000}	pegasus	compat-game
\.


--
-- Data for Name: token_entry; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.token_entry (processor_name, segment, mask, owner, "timestamp", token, token_type) FROM stdin;
__config	0	0	\N	2026-10-12T15:37:51.653Z	16427	org.axonframework.messaging.eventhandling.processing.streaming.token.store.ConfigToken
Automation_WhenWeekStartedThenProclaimWeekSymbol_Processor	4	15	\N	2026-10-12T15:37:57.121Z	16478	org.axonframework.messaging.eventhandling.processing.streaming.token.ReplayToken
ReadModel_Dwelling	0	0	\N	2026-10-12T15:37:52.417Z	16514	org.axonframework.messaging.eventhandling.processing.streaming.token.GapAwareTrackingToken
Automation_WhenCreatureRecruitedThenAddToArmy_Processor	0	15	\N	2026-10-12T15:37:57.081Z	16520	org.axonframework.messaging.eventhandling.processing.streaming.token.GapAwareTrackingToken
Automation_WhenCreatureRecruitedThenAddToArmy_Processor	1	15	\N	2026-10-12T15:37:57.081Z	16519	org.axonframework.messaging.eventhandling.processing.streaming.token.GapAwareTrackingToken
Automation_WhenWeekStartedThenProclaimWeekSymbol_Processor	5	15	\N	2026-10-12T15:37:57.121Z	16480	org.axonframework.messaging.eventhandling.processing.streaming.token.ReplayToken
Automation_WhenWeekStartedThenProclaimWeekSymbol_Processor	6	15	\N	2026-10-12T15:37:57.121Z	16483	org.axonframework.messaging.eventhandling.processing.streaming.token.ReplayToken
Automation_WhenWeekStartedThenProclaimWeekSymbol_Processor	8	15	\N	2026-10-12T15:37:57.122Z	16486	org.axonframework.messaging.eventhandling.processing.streaming.token.ReplayToken
Automation_WhenWeekStartedThenProclaimWeekSymbol_Processor	7	15	\N	2026-10-12T15:37:57.121Z	16485	org.axonframework.messaging.eventhandling.processing.streaming.token.ReplayToken
Automation_WhenWeekStartedThenProclaimWeekSymbol_Processor	9	15	\N	2026-10-12T15:37:57.122Z	16487	org.axonframework.messaging.eventhandling.processing.streaming.token.ReplayToken
Automation_WhenWeekStartedThenProclaimWeekSymbol_Processor	10	15	\N	2026-10-12T15:37:57.122Z	16488	org.axonframework.messaging.eventhandling.processing.streaming.token.ReplayToken
Automation_WhenWeekStartedThenProclaimWeekSymbol_Processor	12	15	\N	2026-10-12T15:37:57.123Z	16490	org.axonframework.messaging.eventhandling.processing.streaming.token.ReplayToken
Automation_WhenWeekStartedThenProclaimWeekSymbol_Processor	11	15	\N	2026-10-12T15:37:57.122Z	16489	org.axonframework.messaging.eventhandling.processing.streaming.token.ReplayToken
Automation_WhenWeekStartedThenProclaimWeekSymbol_Processor	14	15	\N	2026-10-12T15:37:57.124Z	16492	org.axonframework.messaging.eventhandling.processing.streaming.token.ReplayToken
Automation_WhenWeekStartedThenProclaimWeekSymbol_Processor	13	15	\N	2026-10-12T15:37:57.124Z	16491	org.axonframework.messaging.eventhandling.processing.streaming.token.ReplayToken
Automation_WhenWeekStartedThenProclaimWeekSymbol_Processor	15	15	\N	2026-10-12T15:37:57.124Z	16493	org.axonframework.messaging.eventhandling.processing.streaming.token.ReplayToken
Automation_WhenWeekStartedThenProclaimWeekSymbol_Processor	0	15	\N	2026-10-12T15:37:57.119Z	16469	org.axonframework.messaging.eventhandling.processing.streaming.token.ReplayToken
Automation_WhenCreatureRecruitedThenAddToArmy_Processor	5	15	\N	2026-10-12T15:37:57.087Z	16522	org.axonframework.messaging.eventhandling.processing.streaming.token.GapAwareTrackingToken
Automation_WhenCreatureRecruitedThenAddToArmy_Processor	7	15	\N	2026-10-12T15:37:57.087Z	16524	org.axonframework.messaging.eventhandling.processing.streaming.token.GapAwareTrackingToken
Automation_WhenCreatureRecruitedThenAddToArmy_Processor	12	15	\N	2026-10-12T15:37:57.094Z	16531	org.axonframework.messaging.eventhandling.processing.streaming.token.GapAwareTrackingToken
Automation_WhenCreatureRecruitedThenAddToArmy_Processor	10	15	\N	2026-10-12T15:37:57.090Z	16528	org.axonframework.messaging.eventhandling.processing.streaming.token.GapAwareTrackingToken
Automation_WhenWeekStartedThenProclaimWeekSymbol_Processor	1	15	\N	2026-10-12T15:37:57.119Z	16470	org.axonframework.messaging.eventhandling.processing.streaming.token.ReplayToken
Automation_WhenWeekStartedThenProclaimWeekSymbol_Processor	2	15	\N	2026-10-12T15:37:57.119Z	16473	org.axonframework.messaging.eventhandling.processing.streaming.token.ReplayToken
Automation_WhenCreatureRecruitedThenAddToArmy_Processor	3	15	\N	2026-10-12T15:37:57.081Z	16521	org.axonframework.messaging.eventhandling.processing.streaming.token.GapAwareTrackingToken
Automation_WhenCreatureRecruitedThenAddToArmy_Processor	6	15	\N	2026-10-12T15:37:57.087Z	16525	org.axonframework.messaging.eventhandling.processing.streaming.token.GapAwareTrackingToken
Automation_WhenCreatureRecruitedThenAddToArmy_Processor	8	15	\N	2026-10-12T15:37:57.090Z	16527	org.axonframework.messaging.eventhandling.processing.streaming.token.GapAwareTrackingToken
Automation_WhenCreatureRecruitedThenAddToArmy_Processor	11	15	\N	2026-10-12T15:37:57.091Z	16529	org.axonframework.messaging.eventhandling.processing.streaming.token.GapAwareTrackingToken
Automation_WhenCreatureRecruitedThenAddToArmy_Processor	13	15	\N	2026-10-12T15:37:52.432Z	16517	org.axonframework.messaging.eventhandling.processing.streaming.token.GapAwareTrackingToken
Automation_WhenCreatureRecruitedThenAddToArmy_Processor	15	15	\N	2026-10-12T15:37:57.094Z	16532	org.axonframework.messaging.eventhandling.processing.streaming.token.GapAwareTrackingToken
Automation_WhenCreatureRecruitedThenAddToArmy_Processor	14	15	\N	2026-10-12T15:37:57.094Z	16530	org.axonframework.messaging.eventhandling.processing.streaming.token.GapAwareTrackingToken
Automation_WhenCreatureRecruitedThenAddToArmy_Processor	2	15	\N	2026-10-12T15:37:57.081Z	16518	org.axonframework.messaging.eventhandling.processing.streaming.token.GapAwareTrackingToken
Automation_WhenWeekStartedThenProclaimWeekSymbol_Processor	3	15	\N	2026-10-12T15:37:57.119Z	16475	org.axonframework.messaging.eventhandling.processing.streaming.token.ReplayToken
Automation_WhenCreatureRecruitedThenAddToArmy_Processor	4	15	\N	2026-10-12T15:37:57.087Z	16523	org.axonframework.messaging.eventhandling.processing.streaming.token.GapAwareTrackingToken
Automation_WhenCreatureRecruitedThenAddToArmy_Processor	9	15	\N	2026-10-12T15:37:57.090Z	16526	org.axonframework.messaging.eventhandling.processing.streaming.token.GapAwareTrackingToken
\.


--
-- Name: aggregate-event-global-index-sequence; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."aggregate-event-global-index-sequence"', 8, true);


--
-- Data for Name: BLOBS; Type: BLOBS; Schema: -; Owner: -
--

BEGIN;

SELECT pg_catalog.lo_open('16427', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22636f6e666967223a7b226964223a2235613066643561372d366237642d343637642d626363322d626432366137656464393466227d7d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16428', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16429', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16430', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16431', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16432', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16433', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16434', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16435', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16436', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16437', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16438', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16439', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16440', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16441', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16442', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16443', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16444', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16445', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16446', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16447', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16448', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16449', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16450', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16451', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16452', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16453', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16454', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16455', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16456', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16457', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16458', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16459', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16460', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16461', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16462', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16463', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16464', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16465', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16466', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16467', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16468', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16469', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16470', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16471', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16472', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16473', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16474', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16475', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16476', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16477', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16478', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16479', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16480', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16481', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16482', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16483', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16484', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16485', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16486', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16487', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16488', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16489', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16490', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16491', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16492', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16493', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22746f6b656e41745265736574223a7b2240636c617373223a226f72672e61786f6e6672616d65776f726b2e6d6573736167696e672e6576656e7468616e646c696e672e70726f63657373696e672e73747265616d696e672e746f6b656e2e4761704177617265547261636b696e67546f6b656e222c22696e646578223a2d312c2267617073223a5b5d7d2c2263757272656e74546f6b656e223a6e756c6c2c227265736574436f6e74657874223a22227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16494', 131072);
SELECT pg_catalog.lowrite(0, '\x7b2267616d654964223a22636f6d7061742d67616d65222c22636175736174696f6e4964223a2266613031373433642d616633332d346634322d626364642d383862663639613965356537222c22636f7272656c6174696f6e4964223a2266613031373433642d616633332d346634322d626364642d383862663639613965356537222c22706c617965724964223a22706c617965722d31227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16495', 131072);
SELECT pg_catalog.lowrite(0, '\x7b226477656c6c696e674964223a224477656c6c696e673a6477656c6c696e672d31222c2263726561747572654964223a22616e67656c222c22636f737450657254726f6f70223a7b22474f4c44223a333030302c2247454d53223a317d7d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16496', 131072);
SELECT pg_catalog.lowrite(0, '\x7b2267616d654964223a22636f6d7061742d67616d65222c22636175736174696f6e4964223a2231623265383562622d393535312d343537652d623538312d383436643862353032656662222c22636f7272656c6174696f6e4964223a2231623265383562622d393535312d343537652d623538312d383436643862353032656662222c22706c617965724964223a22706c617965722d31227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16497', 131072);
SELECT pg_catalog.lowrite(0, '\x7b226477656c6c696e674964223a224477656c6c696e673a6477656c6c696e672d32222c2263726561747572654964223a2270656761737573222c22636f737450657254726f6f70223a7b22474f4c44223a313030307d7d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16498', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22696e646578223a312c2267617073223a5b305d7d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16499', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22696e646578223a322c2267617073223a5b305d7d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16500', 131072);
SELECT pg_catalog.lowrite(0, '\x7b2267616d654964223a22636f6d7061742d67616d65222c22636175736174696f6e4964223a2230323639313032382d366261362d343565322d616135632d313337653638353936653435222c22636f7272656c6174696f6e4964223a2230323639313032382d366261362d343565322d616135632d313337653638353936653435222c22706c617965724964223a22706c617965722d31227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16501', 131072);
SELECT pg_catalog.lowrite(0, '\x7b226477656c6c696e674964223a224477656c6c696e673a6477656c6c696e672d31222c2263726561747572654964223a22616e67656c222c226368616e676564546f223a357d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16502', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22696e646578223a332c2267617073223a5b305d7d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16503', 131072);
SELECT pg_catalog.lowrite(0, '\x7b2267616d654964223a22636f6d7061742d67616d65222c22636175736174696f6e4964223a2239306435313138362d326236342d343732352d626439652d646533613964656633396635222c22636f7272656c6174696f6e4964223a2239306435313138362d326236342d343732352d626439652d646533613964656633396635222c22706c617965724964223a22706c617965722d31227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16504', 131072);
SELECT pg_catalog.lowrite(0, '\x7b226477656c6c696e674964223a224477656c6c696e673a6477656c6c696e672d32222c2263726561747572654964223a2270656761737573222c226368616e676564546f223a357d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16505', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22696e646578223a342c2267617073223a5b305d7d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16506', 131072);
SELECT pg_catalog.lowrite(0, '\x7b2267616d654964223a22636f6d7061742d67616d65222c22636175736174696f6e4964223a2236376533323636392d383030662d343633322d386436342d623138643437663532343361222c22636f7272656c6174696f6e4964223a2236376533323636392d383030662d343633322d386436342d623138643437663532343361222c22706c617965724964223a22706c617965722d31227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16507', 131072);
SELECT pg_catalog.lowrite(0, '\x7b226477656c6c696e674964223a224477656c6c696e673a6477656c6c696e672d31222c2263726561747572654964223a22616e67656c222c22746f41726d79223a2241726d793a61726d792d31222c227175616e74697479223a332c22746f74616c436f7374223a7b22474f4c44223a393030302c2247454d53223a337d7d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16508', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22696e646578223a352c2267617073223a5b305d7d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16509', 131072);
SELECT pg_catalog.lowrite(0, '\x7b2267616d654964223a22636f6d7061742d67616d65222c22636175736174696f6e4964223a2237316537646465362d373434642d343733622d613631632d373733656534343566356230222c22636f7272656c6174696f6e4964223a2236376533323636392d383030662d343633322d386436342d623138643437663532343361222c22706c617965724964223a22706c617965722d31227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16510', 131072);
SELECT pg_catalog.lowrite(0, '\x7b2261726d794964223a2241726d793a61726d792d31222c2263726561747572654964223a22616e67656c222c227175616e74697479223a337d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16511', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22696e646578223a352c2267617073223a5b305d7d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16512', 131072);
SELECT pg_catalog.lowrite(0, '\x7b2267616d654964223a22636f6d7061742d67616d65222c22636175736174696f6e4964223a2266343237623338652d386665312d343233302d623964362d633031373139333036373262222c22636f7272656c6174696f6e4964223a2266343237623338652d386665312d343233302d623964362d633031373139333036373262222c22706c617965724964223a22706c617965722d31227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16513', 131072);
SELECT pg_catalog.lowrite(0, '\x7b226477656c6c696e674964223a224477656c6c696e673a6477656c6c696e672d32222c2263726561747572654964223a2270656761737573222c22746f41726d79223a2241726d793a61726d792d31222c227175616e74697479223a322c22746f74616c436f7374223a7b22474f4c44223a323030307d7d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16514', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22696e646578223a372c2267617073223a5b305d7d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16515', 131072);
SELECT pg_catalog.lowrite(0, '\x7b2267616d654964223a22636f6d7061742d67616d65222c22636175736174696f6e4964223a2238353431666263362d353935652d346234322d613736362d346337623831666366613461222c22636f7272656c6174696f6e4964223a2266343237623338652d386665312d343233302d623964362d633031373139333036373262222c22706c617965724964223a22706c617965722d31227d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16516', 131072);
SELECT pg_catalog.lowrite(0, '\x7b2261726d794964223a2241726d793a61726d792d31222c2263726561747572654964223a2270656761737573222c227175616e74697479223a327d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16517', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22696e646578223a372c2267617073223a5b305d7d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16518', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22696e646578223a372c2267617073223a5b305d7d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16519', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22696e646578223a372c2267617073223a5b305d7d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16520', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22696e646578223a372c2267617073223a5b305d7d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16521', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22696e646578223a372c2267617073223a5b305d7d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16522', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22696e646578223a372c2267617073223a5b305d7d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16523', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22696e646578223a372c2267617073223a5b305d7d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16524', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22696e646578223a372c2267617073223a5b305d7d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16525', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22696e646578223a372c2267617073223a5b305d7d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16526', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22696e646578223a372c2267617073223a5b305d7d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16527', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22696e646578223a372c2267617073223a5b305d7d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16528', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22696e646578223a372c2267617073223a5b305d7d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16529', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22696e646578223a372c2267617073223a5b305d7d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16530', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22696e646578223a372c2267617073223a5b305d7d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16531', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22696e646578223a372c2267617073223a5b305d7d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('16532', 131072);
SELECT pg_catalog.lowrite(0, '\x7b22696e646578223a372c2267617073223a5b305d7d');
SELECT pg_catalog.lo_close(0);

COMMIT;

--
-- Name: aggregate_event_entry aggregate_event_entry_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aggregate_event_entry
    ADD CONSTRAINT aggregate_event_entry_pkey PRIMARY KEY (global_index);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: read_model_built_dwelling read_model_built_dwelling_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.read_model_built_dwelling
    ADD CONSTRAINT read_model_built_dwelling_pkey PRIMARY KEY (dwelling_id);


--
-- Name: read_model_dwelling read_model_dwelling_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.read_model_dwelling
    ADD CONSTRAINT read_model_dwelling_pkey PRIMARY KEY (dwelling_id);


--
-- Name: token_entry token_entry_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.token_entry
    ADD CONSTRAINT token_entry_pkey PRIMARY KEY (processor_name, segment);


--
-- Name: aggregate_event_entry uk7lm0ppm66bd86judy5vot9s42; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aggregate_event_entry
    ADD CONSTRAINT uk7lm0ppm66bd86judy5vot9s42 UNIQUE (aggregate_identifier, aggregate_sequence_number);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: idx_read_model_built_dwelling_game_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_read_model_built_dwelling_game_id ON public.read_model_built_dwelling USING btree (game_id);


--
-- Name: idx_read_model_dwelling_game_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_read_model_dwelling_game_id ON public.read_model_dwelling USING btree (game_id);


--
-- PostgreSQL database dump complete
--

\unrestrict 7uZifbNC689GFBEpPg7CxYUk3Pfub3tPjoVKzz4Jbqbj5nMuggnjfCi4cd9FgR5

