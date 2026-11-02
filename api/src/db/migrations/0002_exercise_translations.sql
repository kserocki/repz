CREATE TABLE "exercise_translations" (
	"exercise_id" uuid NOT NULL,
	"locale" text NOT NULL,
	"name" text NOT NULL,
	"instructions" text,
	CONSTRAINT "exercise_translations_pkey" PRIMARY KEY("exercise_id","locale")
);
--> statement-breakpoint
ALTER TABLE "exercises" ADD COLUMN "source_url" text;--> statement-breakpoint
ALTER TABLE "exercise_translations" ADD CONSTRAINT "exercise_translations_exercise_id_fkey" FOREIGN KEY ("exercise_id") REFERENCES "public"."exercises"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "exercises" ADD CONSTRAINT "exercises_primary_muscle_check" CHECK ("exercises"."primary_muscle" in ('chest', 'back', 'shoulders', 'biceps', 'triceps', 'forearms', 'abs', 'glutes', 'quadriceps', 'hamstrings', 'calves', 'full_body'));--> statement-breakpoint
ALTER TABLE "exercises" ADD CONSTRAINT "exercises_secondary_muscles_check" CHECK ("exercises"."secondary_muscles" <@ array['chest', 'back', 'shoulders', 'biceps', 'triceps', 'forearms', 'abs', 'glutes', 'quadriceps', 'hamstrings', 'calves', 'full_body']::text[]);