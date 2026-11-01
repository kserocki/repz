CREATE SEQUENCE "public"."sync_version_seq" INCREMENT BY 1 MINVALUE 1 MAXVALUE 9223372036854775807 START WITH 1 CACHE 1;--> statement-breakpoint
CREATE TABLE "exercises" (
	"id" uuid PRIMARY KEY NOT NULL,
	"owner_id" uuid,
	"name" text NOT NULL,
	"primary_muscle" text NOT NULL,
	"secondary_muscles" text[] DEFAULT '{}' NOT NULL,
	"equipment" text NOT NULL,
	"license" text,
	"author" text,
	"version" bigint DEFAULT nextval('sync_version_seq') NOT NULL,
	"updated_at" timestamp (3) with time zone DEFAULT now() NOT NULL,
	"deleted_at" timestamp (3) with time zone,
	CONSTRAINT "exercises_equipment_check" CHECK ("exercises"."equipment" in ('barbell', 'dumbbell', 'kettlebell', 'machine', 'cable', 'bodyweight', 'band', 'other'))
);
--> statement-breakpoint
CREATE TABLE "template_exercises" (
	"template_id" uuid NOT NULL,
	"position" integer NOT NULL,
	"exercise_id" uuid NOT NULL,
	"target_sets" integer NOT NULL,
	"target_reps_min" integer,
	"target_reps_max" integer,
	"rest_seconds" integer NOT NULL,
	CONSTRAINT "template_exercises_pkey" PRIMARY KEY("template_id","position"),
	CONSTRAINT "template_exercises_position_check" CHECK ("template_exercises"."position" >= 0),
	CONSTRAINT "template_exercises_target_sets_check" CHECK ("template_exercises"."target_sets" >= 1),
	CONSTRAINT "template_exercises_target_reps_check" CHECK (("template_exercises"."target_reps_min" is null and "template_exercises"."target_reps_max" is null)
          or ("template_exercises"."target_reps_min" is not null and "template_exercises"."target_reps_max" is not null
              and "template_exercises"."target_reps_min" >= 1 and "template_exercises"."target_reps_min" <= "template_exercises"."target_reps_max")),
	CONSTRAINT "template_exercises_rest_seconds_check" CHECK ("template_exercises"."rest_seconds" >= 0)
);
--> statement-breakpoint
CREATE TABLE "workout_templates" (
	"id" uuid PRIMARY KEY NOT NULL,
	"user_id" uuid NOT NULL,
	"name" text NOT NULL,
	"notes" text DEFAULT '' NOT NULL,
	"version" bigint DEFAULT nextval('sync_version_seq') NOT NULL,
	"updated_at" timestamp (3) with time zone DEFAULT now() NOT NULL,
	"deleted_at" timestamp (3) with time zone
);
--> statement-breakpoint
CREATE TABLE "refresh_tokens" (
	"id" uuid PRIMARY KEY DEFAULT uuidv7() NOT NULL,
	"user_id" uuid NOT NULL,
	"family_id" uuid NOT NULL,
	"token_hash" text NOT NULL,
	"created_at" timestamp (3) with time zone DEFAULT now() NOT NULL,
	"expires_at" timestamp (3) with time zone NOT NULL,
	"revoked_at" timestamp (3) with time zone
);
--> statement-breakpoint
CREATE TABLE "users" (
	"id" uuid PRIMARY KEY DEFAULT uuidv7() NOT NULL,
	"email" text NOT NULL,
	"password_hash" text NOT NULL,
	"display_name" text NOT NULL,
	"unit_system" text DEFAULT 'kg' NOT NULL,
	"created_at" timestamp (3) with time zone DEFAULT now() NOT NULL,
	"updated_at" timestamp (3) with time zone DEFAULT now() NOT NULL,
	CONSTRAINT "users_unit_system_check" CHECK ("users"."unit_system" in ('kg', 'lb'))
);
--> statement-breakpoint
CREATE TABLE "workout_exercises" (
	"id" uuid PRIMARY KEY NOT NULL,
	"workout_id" uuid NOT NULL,
	"exercise_id" uuid NOT NULL,
	"position" integer NOT NULL,
	"notes" text DEFAULT '' NOT NULL,
	CONSTRAINT "workout_exercises_position_check" CHECK ("workout_exercises"."position" >= 0)
);
--> statement-breakpoint
CREATE TABLE "workout_sets" (
	"id" uuid PRIMARY KEY NOT NULL,
	"workout_exercise_id" uuid NOT NULL,
	"position" integer NOT NULL,
	"type" text NOT NULL,
	"weight_grams" bigint NOT NULL,
	"reps" integer NOT NULL,
	"rpe" numeric(3, 1),
	"completed_at" timestamp (3) with time zone,
	CONSTRAINT "workout_sets_type_check" CHECK ("workout_sets"."type" in ('warmup', 'normal', 'drop', 'failure')),
	CONSTRAINT "workout_sets_weight_grams_check" CHECK ("workout_sets"."weight_grams" >= 0),
	CONSTRAINT "workout_sets_reps_check" CHECK ("workout_sets"."reps" >= 0),
	CONSTRAINT "workout_sets_rpe_check" CHECK ("workout_sets"."rpe" is null or "workout_sets"."rpe" between 1 and 10),
	CONSTRAINT "workout_sets_position_check" CHECK ("workout_sets"."position" >= 0)
);
--> statement-breakpoint
CREATE TABLE "workouts" (
	"id" uuid PRIMARY KEY NOT NULL,
	"user_id" uuid NOT NULL,
	"template_id" uuid,
	"name" text NOT NULL,
	"notes" text DEFAULT '' NOT NULL,
	"started_at" timestamp (3) with time zone NOT NULL,
	"finished_at" timestamp (3) with time zone NOT NULL,
	"version" bigint DEFAULT nextval('sync_version_seq') NOT NULL,
	"updated_at" timestamp (3) with time zone DEFAULT now() NOT NULL,
	"deleted_at" timestamp (3) with time zone,
	CONSTRAINT "workouts_finished_at_check" CHECK ("workouts"."finished_at" >= "workouts"."started_at")
);
--> statement-breakpoint
ALTER TABLE "exercises" ADD CONSTRAINT "exercises_owner_id_fkey" FOREIGN KEY ("owner_id") REFERENCES "public"."users"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "template_exercises" ADD CONSTRAINT "template_exercises_template_id_fkey" FOREIGN KEY ("template_id") REFERENCES "public"."workout_templates"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "workout_templates" ADD CONSTRAINT "workout_templates_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."users"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "refresh_tokens" ADD CONSTRAINT "refresh_tokens_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."users"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "workout_exercises" ADD CONSTRAINT "workout_exercises_workout_id_fkey" FOREIGN KEY ("workout_id") REFERENCES "public"."workouts"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "workout_sets" ADD CONSTRAINT "workout_sets_workout_exercise_id_fkey" FOREIGN KEY ("workout_exercise_id") REFERENCES "public"."workout_exercises"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "workouts" ADD CONSTRAINT "workouts_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."users"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
CREATE INDEX "exercises_owner_id_version_idx" ON "exercises" USING btree ("owner_id","version");--> statement-breakpoint
CREATE INDEX "exercises_catalog_version_idx" ON "exercises" USING btree ("version") WHERE "exercises"."owner_id" is null;--> statement-breakpoint
CREATE INDEX "workout_templates_user_id_version_idx" ON "workout_templates" USING btree ("user_id","version");--> statement-breakpoint
CREATE UNIQUE INDEX "refresh_tokens_token_hash_key" ON "refresh_tokens" USING btree ("token_hash");--> statement-breakpoint
CREATE INDEX "refresh_tokens_user_id_idx" ON "refresh_tokens" USING btree ("user_id");--> statement-breakpoint
CREATE INDEX "refresh_tokens_family_id_idx" ON "refresh_tokens" USING btree ("family_id");--> statement-breakpoint
CREATE UNIQUE INDEX "users_email_lower_key" ON "users" USING btree (lower("email"));--> statement-breakpoint
CREATE UNIQUE INDEX "workout_exercises_workout_id_position_key" ON "workout_exercises" USING btree ("workout_id","position");--> statement-breakpoint
CREATE UNIQUE INDEX "workout_sets_workout_exercise_id_position_key" ON "workout_sets" USING btree ("workout_exercise_id","position");--> statement-breakpoint
CREATE INDEX "workouts_user_id_version_idx" ON "workouts" USING btree ("user_id","version");