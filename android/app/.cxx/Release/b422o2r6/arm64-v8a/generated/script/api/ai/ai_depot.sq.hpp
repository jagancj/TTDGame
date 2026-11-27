/*
 * This file is part of OpenTTD.
 * OpenTTD is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, version 2.
 * OpenTTD is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with OpenTTD. If not, see <https://www.gnu.org/licenses/old-licenses/gpl-2.0>.
 */

/**
 * @file ai_depot.sq.hpp Exposes ottd c++ api to squirrel.
 * @attention THIS FILE IS AUTO-GENERATED. PLEASE DO NOT ALTER MANUALLY.
 */


#include "../script_depot.hpp"
#include "../template/template_depot.sq.hpp"


template <> SQInteger PushClassName<ScriptDepot, ScriptType::AI>(HSQUIRRELVM vm) { sq_pushstring(vm, "AIDepot"); return 1; }

void SQAIDepot_Register(Squirrel &engine)
{
	DefSQClass<ScriptDepot, ScriptType::AI> SQAIDepot("AIDepot");
	SQAIDepot.PreRegister(engine, "AIObject");

	SQAIDepot.DefSQConst(engine, ScriptDepot::ERR_DEPOT_BASE, "ERR_DEPOT_BASE");
	SQAIDepot.DefSQStaticMethod(engine, &ScriptDepot::GetName, "GetName", ".i");
	SQAIDepot.DefSQStaticMethod(engine, &ScriptDepot::SetName, "SetName", ".i.");

	SQAIDepot.PostRegister(engine);
}
